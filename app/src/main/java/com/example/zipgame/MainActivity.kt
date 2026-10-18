package com.example.zipgame

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/* ---------------------------------------------------------------------------------------------
 * Palette (LinkedIn-inspired: white surface, restrained blue accent, soft greys)
 * ------------------------------------------------------------------------------------------- */
private val Blue = Color(0xFF0A66C2)
private val DarkBlue = Color(0xFF004182)
private val SoftBlue = Color(0xFFE3EFFA)
private val Ink = Color(0xFF191919)
private val GreyText = Color(0xFF666666)
private val GridGrey = Color(0xFFD6DADF)
private val PaleGrey = Color(0xFFF3F2EF)
private val Success = Color(0xFF057642)
private val SoftSuccess = Color(0xFFE6F4EC)
private val ErrorRed = Color(0xFFB24020)
private val SoftError = Color(0xFFFBEBE6)
private val HintFill = Color(0xFFFFF0C7)
private val HintRing = Color(0xFFE0A030)

/* ---------------------------------------------------------------------------------------------
 * Puzzle model
 *
 * Cells are indexed row-major: index = row * size + column.
 * `checkpoints` maps a cell index to its number (1..k). `solution` is a verified Hamiltonian route.
 * Rules (standard Zip): one continuous orthogonal path from 1 that visits the numbers in order,
 * covers EVERY cell exactly once, and finishes on the highest number.
 * Difference from LinkedIn: this version has no wall segments between cells.
 * ------------------------------------------------------------------------------------------- */
private data class Level(
    val size: Int,
    val checkpoints: Map<Int, Int>,
    val solution: List<Int>
)

private fun neighborsOf(cell: Int, n: Int): List<Int> {
    val r = cell / n
    val c = cell % n
    val out = ArrayList<Int>(4)
    if (r > 0) out.add(cell - n)
    if (r < n - 1) out.add(cell + n)
    if (c > 0) out.add(cell - 1)
    if (c < n - 1) out.add(cell + 1)
    return out
}

private fun isAdjacent(a: Int, b: Int, n: Int): Boolean =
    abs(a / n - b / n) + abs(a % n - b % n) == 1

/** Checks that the stored solution really satisfies every Zip rule. */
private fun Level.isSolutionValid(): Boolean {
    val total = size * size
    if (solution.size != total) return false
    if (solution.any { it !in 0 until total }) return false
    if (solution.toSet().size != total) return false
    if (solution.zipWithNext().any { (a, b) -> !isAdjacent(a, b, size) }) return false
    val k = checkpoints.size
    if (solution.mapNotNull { checkpoints[it] } != (1..k).toList()) return false
    return checkpoints[solution.last()] == k
}

// Levels were generated from random Hamiltonian paths and each was cross-checked with an
// independent solver (every level has between 1 and 4 solutions). They are re-validated at startup.
private val RAW_LEVELS: List<Level> = listOf(
    Level(
        4,
        mapOf(13 to 1, 5 to 2, 3 to 3, 10 to 4, 14 to 5),
        listOf(13, 12, 8, 9, 5, 4, 0, 1, 2, 3, 7, 6, 10, 11, 15, 14)
    ),
    Level(
        5,
        mapOf(18 to 1, 1 to 2, 7 to 3, 15 to 4, 16 to 5, 10 to 6),
        listOf(18, 23, 24, 19, 14, 9, 4, 3, 2, 1, 0, 5, 6, 7, 8, 13, 12, 17, 22, 21, 20, 15, 16, 11, 10)
    ),
    Level(
        5,
        mapOf(22 to 1, 19 to 2, 4 to 3, 18 to 4, 0 to 5, 5 to 6, 10 to 7, 16 to 8),
        listOf(22, 23, 24, 19, 14, 9, 4, 3, 8, 13, 18, 17, 12, 7, 2, 1, 0, 5, 6, 11, 10, 15, 20, 21, 16)
    ),
    Level(
        6,
        mapOf(23 to 1, 27 to 2, 6 to 3, 17 to 4, 9 to 5, 8 to 6, 25 to 7, 20 to 8),
        listOf(
            23, 29, 35, 34, 28, 22, 21, 27, 33, 32, 31, 30, 24, 18, 12, 6, 0, 1,
            2, 3, 4, 5, 11, 17, 16, 10, 9, 15, 14, 8, 7, 13, 19, 25, 26, 20
        )
    ),
    Level(
        6,
        mapOf(22 to 1, 18 to 2, 12 to 3, 1 to 4, 20 to 5, 14 to 6, 16 to 7, 11 to 8, 5 to 9, 4 to 10),
        listOf(
            22, 23, 29, 35, 34, 28, 27, 33, 32, 26, 25, 31, 30, 24, 18, 12, 6, 0,
            1, 7, 13, 19, 20, 21, 15, 14, 8, 2, 3, 9, 10, 16, 17, 11, 5, 4
        )
    ),
    Level(
        7,
        mapOf(12 to 1, 48 to 2, 7 to 3, 4 to 4, 8 to 5, 19 to 6, 37 to 7, 31 to 8, 39 to 9, 32 to 10),
        listOf(
            12, 5, 6, 13, 20, 27, 34, 41, 48, 47, 46, 45, 44, 43, 42, 35, 28, 21, 14, 7, 0, 1, 2, 3, 4,
            11, 10, 9, 8, 15, 16, 17, 18, 19, 26, 25, 24, 23, 22, 29, 36, 37, 30, 31, 38, 39, 40, 33, 32
        )
    ),
    Level(
        7,
        mapOf(
            38 to 1, 31 to 2, 18 to 3, 39 to 4, 26 to 5, 5 to 6,
            11 to 7, 9 to 8, 7 to 9, 15 to 10, 29 to 11, 36 to 12
        ),
        listOf(
            38, 45, 44, 37, 30, 31, 24, 23, 16, 17, 18, 25, 32, 39, 46, 47, 48, 41, 40, 33, 34, 27, 26, 19, 20,
            13, 6, 5, 12, 11, 4, 3, 10, 9, 2, 1, 0, 7, 8, 15, 14, 21, 22, 29, 28, 35, 42, 43, 36
        )
    )
)

private val LEVELS: List<Level> = RAW_LEVELS.filter { it.isSolutionValid() }

/* ---------------------------------------------------------------------------------------------
 * Solver (used by Hint when the player has left the stored solution)
 * Depth-first search with connectivity pruning and a node budget.
 * ------------------------------------------------------------------------------------------- */
private class ZipSolver(
    private val level: Level,
    prefix: List<Int>,
    private val budget: Int = 400_000
) {
    private val n = level.size
    private val total = n * n
    private val lastNumber = level.checkpoints.values.maxOrNull() ?: 0
    private val visited = BooleanArray(total)
    private val route = ArrayList<Int>(total)
    private var nextNum = 2
    private var nodes = 0

    init {
        prefix.forEach { visited[it] = true; route.add(it) }
        nextNum = prefix.count { level.checkpoints.containsKey(it) } + 1
    }

    fun solve(): List<Int>? = if (route.isNotEmpty() && dfs()) route.toList() else null

    private fun reachableCount(from: Int): Int {
        val seen = BooleanArray(total)
        val stack = ArrayList<Int>()
        seen[from] = true
        stack.add(from)
        var count = 0
        while (stack.isNotEmpty()) {
            val c = stack.removeAt(stack.lastIndex)
            for (nb in neighborsOf(c, n)) {
                if (!visited[nb] && !seen[nb]) {
                    seen[nb] = true
                    count++
                    stack.add(nb)
                }
            }
        }
        return count
    }

    private fun dfs(): Boolean {
        if (++nodes > budget) return false
        if (route.size == total) return level.checkpoints[route.last()] == lastNumber
        for (nb in neighborsOf(route.last(), n)) {
            if (visited[nb]) continue
            val num = level.checkpoints[nb]
            if (num != null && num != nextNum) continue
            if (num == lastNumber && route.size + 1 != total) continue
            visited[nb] = true
            route.add(nb)
            if (num != null) nextNum++
            val remaining = total - route.size
            if ((remaining == 0 || reachableCount(nb) == remaining) && dfs()) return true
            if (num != null) nextNum--
            route.removeAt(route.lastIndex)
            visited[nb] = false
        }
        return false
    }
}

/* ---------------------------------------------------------------------------------------------
 * Game state (rules live here; the UI only renders it and forwards input)
 * ------------------------------------------------------------------------------------------- */
private enum class MessageKind { INFO, ERROR, SUCCESS, HINT }

private data class StatusMessage(val text: String, val kind: MessageKind)

@Stable
private class ZipGameState(val level: Level) {
    val total: Int = level.size * level.size
    private val lastNumber: Int = level.checkpoints.values.maxOrNull() ?: 0

    val path = mutableStateListOf<Int>()

    var hint by mutableStateOf<List<Int>>(emptyList())
        private set
    var status by mutableStateOf(
        StatusMessage("Start on checkpoint 1, then drag or tap neighbouring cells.", MessageKind.INFO)
    )
        private set

    val completed: Boolean
        get() = path.size == total && level.checkpoints[path.last()] == lastNumber

    val nextCheckpoint: Int
        get() = path.count { level.checkpoints.containsKey(it) } + 1

    private fun info(text: String) { status = StatusMessage(text, MessageKind.INFO) }
    private fun error(text: String) { status = StatusMessage(text, MessageKind.ERROR) }

    private fun progressInfo() {
        val next = nextCheckpoint
        info(
            if (next <= lastNumber) "${path.size} of $total cells · next: checkpoint $next"
            else "${path.size} of $total cells · fill the rest and finish on $lastNumber"
        )
    }

    /**
     * Attempts to put [cell] on the path. Returns true if the path changed.
     * Entering a cell already on the path rewinds the path back to that cell.
     */
    fun enter(cell: Int, announce: Boolean): Boolean {
        if (completed) return false
        val n = level.size

        if (path.isEmpty()) {
            return if (level.checkpoints[cell] == 1) {
                path.add(cell)
                hint = emptyList()
                progressInfo()
                true
            } else {
                if (announce) error("The path has to start on checkpoint 1.")
                false
            }
        }

        val end = path.last()
        if (cell == end) return false

        val idx = path.indexOf(cell)
        if (idx >= 0) {
            while (path.size > idx + 1) path.removeAt(path.lastIndex)
            hint = emptyList()
            progressInfo()
            return true
        }

        if (!isAdjacent(end, cell, n)) {
            if (announce) error("Move to a neighbouring cell – no diagonals and no jumping.")
            return false
        }

        val num = level.checkpoints[cell]
        if (num != null && num != nextCheckpoint) {
            error("Reach checkpoint $nextCheckpoint before checkpoint $num.")
            return false
        }
        if (num == lastNumber && path.size + 1 != total) {
            error("Checkpoint $num is the finish – fill every other cell first.")
            return false
        }

        path.add(cell)
        hint = emptyList()
        if (completed) {
            status = StatusMessage("Solved! All $total cells connected in order.", MessageKind.SUCCESS)
        } else {
            progressInfo()
        }
        return true
    }

    fun undo() {
        if (completed) {
            info("This level is solved. Restart it or continue to the next one.")
            return
        }
        if (path.isEmpty()) {
            error("Nothing to undo yet.")
            return
        }
        path.removeAt(path.lastIndex)
        hint = emptyList()
        if (path.isEmpty()) info("Path cleared. Start on checkpoint 1.") else progressInfo()
    }

    fun restart() {
        path.clear()
        hint = emptyList()
        info("Level restarted. Start on checkpoint 1.")
    }

    fun requestHint() {
        if (completed) {
            info("You have already solved this level.")
            return
        }
        val n = level.size
        val sol = level.solution
        val current = path.toList()

        val onStoredSolution = current.size <= sol.size && current.indices.all { current[it] == sol[it] }
        val route: List<Int>? = when {
            current.isEmpty() || onStoredSolution -> sol
            else -> ZipSolver(level, current).solve()
        }

        if (route != null) {
            val upcoming = route.subList(current.size, min(current.size + 3, route.size))
            hint = upcoming
            if (current.isEmpty()) {
                status = StatusMessage("Hint: begin on the highlighted checkpoint 1.", MessageKind.HINT)
            } else {
                val delta = upcoming.first() - current.last()
                val direction = when (delta) {
                    1 -> "right"
                    -1 -> "left"
                    n -> "down"
                    else -> "up"
                }
                val steps = if (upcoming.size > 1) "the highlighted cells" else "the highlighted cell"
                status = StatusMessage(
                    "Hint: move $direction toward checkpoint ${nextCheckpoint}, through $steps.",
                    MessageKind.HINT
                )
            }
        } else {
            // The current route cannot be completed. Point to the last cell that is still safe.
            var common = 0
            while (common < current.size && common < sol.size && current[common] == sol[common]) common++
            if (common == 0) {
                hint = listOf(sol[0])
                status = StatusMessage(
                    "That route cannot be completed. Undo everything and start from checkpoint 1.",
                    MessageKind.HINT
                )
            } else {
                hint = listOf(sol[common - 1])
                status = StatusMessage(
                    "That route cannot be completed. Undo (or tap) back to the highlighted cell.",
                    MessageKind.HINT
                )
            }
        }
    }
}

/* ---------------------------------------------------------------------------------------------
 * Activity
 * ------------------------------------------------------------------------------------------- */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Blue,
                    onPrimary = Color.White,
                    background = Color.White,
                    surface = Color.White,
                    onSurface = Ink
                )
            ) {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
                    ZipApp()
                }
            }
        }
    }
}

/* ---------------------------------------------------------------------------------------------
 * UI
 * ------------------------------------------------------------------------------------------- */
@Composable
private fun ZipApp() {
    var levelIndex by rememberSaveable { mutableIntStateOf((0)) }
    val safeIndex = levelIndex.coerceIn(0, LEVELS.lastIndex)
    val solved = remember { mutableStateListOf<Int>() }
    val state = remember(safeIndex) { ZipGameState(LEVELS[safeIndex]) }

    LaunchedEffect(state.completed, safeIndex) {
        if (state.completed && safeIndex !in solved) solved.add(safeIndex)
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .systemBarsPadding()
    ) {
        val contentWidth = minOf(maxWidth - 40.dp, 440.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(modifier = Modifier.width(contentWidth)) {
                Header(levelNumber = safeIndex + 1, levelCount = LEVELS.size)
                Spacer(Modifier.height(20.dp))
                Text(
                    "Connect the dots",
                    color = Ink,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Draw one path from 1 through every number in order, covering every cell. " +
                            "Drag or tap neighbouring cells; tap the path to step back.",
                    color = GreyText,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                Spacer(Modifier.height(16.dp))
                ProgressSection(done = state.path.size, total = state.total, completed = state.completed)
                Spacer(Modifier.height(14.dp))
                ZipBoard(state = state, modifier = Modifier.size(contentWidth))
                Spacer(Modifier.height(14.dp))
                StatusBar(state.status)
                Spacer(Modifier.height(14.dp))
                Controls(state)
                if (state.completed) {
                    Spacer(Modifier.height(10.dp))
                    val isLast = safeIndex == LEVELS.lastIndex
                    Button(
                        onClick = { levelIndex = if (isLast) 0 else safeIndex + 1 },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Blue)
                    ) {
                        Text(
                            if (isLast) "All levels done – play again" else "Next level",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                    }
                }
                Spacer(Modifier.height(18.dp))
                LevelDots(current = safeIndex, count = LEVELS.size, solved = solved.toList())
            }
        }
    }
}

@Composable
private fun Header(levelNumber: Int, levelCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Small vector glyph: a zig_zag path on a rounded blue tile
        Canvas(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Blue)
        ) {
            val s = size.width
            val p = Path().apply {
                moveTo(s * 0.22f, s * 0.30f)
                lineTo(s * 0.78f, s * 0.30f)
                lineTo(s * 0.22f, s * 0.70f)
                lineTo(s * 0.78f, s * 0.70f)
            }
            drawPath(
                p,
                Color.White,
                style = Stroke(width = s * 0.11f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
        Spacer(Modifier.width(10.dp))
        Text("Zip", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(SoftBlue)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                "Level $levelNumber / $levelCount",
                color = DarkBlue,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ProgressSection(done: Int, total: Int, completed: Boolean) {
    val fraction by animateFloatAsState(
        targetValue = if (total == 0) 0f else done.toFloat() / total,
        animationSpec = tween(250),
        label = "progress"
    )
    val barColor by animateColorAsState(if (completed) Success else Blue, tween(300), label = "bar")
    Column {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Progress", color = GreyText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.weight(1f))
            Text("$done / $total cells", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(PaleGrey)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(barColor)
            )
        }
    }
}

@Composable
private fun ZipBoard(state: ZipGameState, modifier: Modifier = Modifier) {
    val level = state.level
    val n = level.size
    val density = LocalDensity.current
    val pathColor by animateColorAsState(
        targetValue = if (state.completed) Success else Blue,
        animationSpec = tween(400),
        label = "pathColor"
    )
    val shape = RoundedCornerShape(12.dp)

    BoxWithConstraints(
        modifier = modifier
            .clip(shape)
            .background(Color.White)
            .border(1.5.dp, GridGrey, shape)
    ) {
        val boardPx = with(density) { maxWidth.toPx() }
        val cellPx = boardPx / n
        val cellDp = maxWidth / n

        fun cellAt(p: Offset): Int? {
            if (p.x < 0f || p.y < 0f || p.x >= boardPx || p.y >= boardPx) return null
            val col = (p.x / cellPx).toInt().coerceIn(0, n - 1)
            val row = (p.y / cellPx).toInt().coerceIn(0, n - 1)
            return row * n + col
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(state, cellPx) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        down.consume()
                        cellAt(down.position)?.let { state.enter(it, announce = true) }
                        var previous = down.position
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            val target = change.position
                            val dx = target.x - previous.x
                            val dy = target.y - previous.y
                            val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                            // Sample along the finger's movement so fast drags never skip a cell.
                            val steps = max(1, ceil(dist / (cellPx / 4f)).toInt())
                            for (i in 1..steps) {
                                val f = i.toFloat() / steps
                                val sample = Offset(previous.x + dx * f, previous.y + dy * f)
                                val cell = cellAt(sample)
                                if (cell != null && cell != state.path.lastOrNull()) {
                                    state.enter(cell, announce = false)
                                }
                            }
                            previous = target
                            change.consume()
                        }
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cell = size.width / n
                fun origin(c: Int) = Offset((c % n) * cell, (c / n) * cell)
                fun center(c: Int) = Offset((c % n) * cell + cell / 2f, (c / n) * cell + cell / 2f)

                // 1. Soft tint for visited cells and amber for hinted cells
                state.path.forEach { c ->
                    drawRect(SoftBlue, topLeft = origin(c), size = Size(cell, cell))
                }
                val hinted = state.hint
                hinted.forEach { c ->
                    drawRect(HintFill, topLeft = origin(c), size = Size(cell, cell))
                }

                // 2. Grid lines
                val gridStroke = 1.dp.toPx()
                for (i in 1 until n) {
                    drawLine(GridGrey, Offset(i * cell, 0f), Offset(i * cell, size.height), gridStroke)
                    drawLine(GridGrey, Offset(0f, i * cell), Offset(size.width, i * cell), gridStroke)
                }
                hinted.firstOrNull()?.let { c ->
                    drawRect(
                        HintRing,
                        topLeft = origin(c) + Offset(2.dp.toPx(), 2.dp.toPx()),
                        size = Size(cell - 4.dp.toPx(), cell - 4.dp.toPx()),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }

                // 3. Continuous path (behind the checkpoints)
                val p = state.path
                if (p.isNotEmpty()) {
                    val strokeWidth = cell * 0.40f
                    if (p.size > 1) {
                        val route = Path()
                        val first = center(p.first())
                        route.moveTo(first.x, first.y)
                        for (i in 1 until p.size) {
                            val c = center(p[i])
                            route.lineTo(c.x, c.y)
                        }
                        drawPath(
                            route,
                            pathColor,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }
                    drawCircle(pathColor, radius = strokeWidth / 2f, center = center(p.last()))
                }

                // 4. Checkpoint discs
                val next = state.nextCheckpoint
                level.checkpoints.forEach { (c, num) ->
                    val reached = p.contains(c)
                    drawCircle(
                        color = if (reached) DarkBlue else Ink,
                        radius = cell * 0.30f,
                        center = center(c)
                    )
                    if (num == next && !state.completed) {
                        drawCircle(
                            color = Blue,
                            radius = cell * 0.37f,
                            center = center(c),
                            style = Stroke(width = 2.5.dp.toPx())
                        )
                    }
                }
            }

            // 5. Checkpoint numbers on top of path and discs
            level.checkpoints.forEach { (c, num) ->
                key(c) {
                    Box(
                        modifier = Modifier
                            .offset(x = cellDp * (c % n), y = cellDp * (c / n))
                            .size(cellDp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = num.toString(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = with(density) { (cellPx * 0.30f).toSp() },
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBar(message: StatusMessage) {
    val (bg, fg) = when (message.kind) {
        MessageKind.INFO -> PaleGrey to Ink
        MessageKind.ERROR -> SoftError to ErrorRed
        MessageKind.SUCCESS -> SoftSuccess to Success
        MessageKind.HINT -> HintFill to Color(0xFF7A5200)
    }
    val animatedBg by animateColorAsState(bg, tween(200), label = "statusBg")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(animatedBg)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(fg)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = message.text,
            color = fg,
            fontSize = 13.sp,
            lineHeight = 17.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2
        )
    }
}

@Composable
private fun Controls(state: ZipGameState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ControlButton("Undo", Modifier.weight(1f)) { state.undo() }
        ControlButton("Hint", Modifier.weight(1f)) { state.requestHint() }
        ControlButton("Restart", Modifier.weight(1f)) { state.restart() }
    }
}

@Composable
private fun ControlButton(label: String, modifier: Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(22.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Blue),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Blue)
    ) {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
private fun LevelDots(current: Int, count: Int, solved: List<Int>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            for (i in 0 until count) {
                val isSolved = i in solved
                val isCurrent = i == current
                Box(
                    modifier = Modifier
                        .size(if (isCurrent) 12.dp else 9.dp)
                        .clip(CircleShape)
                        .background(if (isSolved) Blue else Color.White)
                        .border(1.5.dp, if (isSolved || isCurrent) Blue else GridGrey, CircleShape)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "${solved.size} of $count levels solved",
            color = GreyText,
            fontSize = 12.sp
        )
    }
}