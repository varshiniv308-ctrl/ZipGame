package com.example.zipgame

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ZipGame()
        }
    }
}

@Composable
fun ZipGame() {

    val gridSize = 6
    val totalCells = 36

    // Number of numbered points in each level
    val numbersPerLevel = listOf(
        6,
        8,
        10,
        12,
        15,
        18,
        21,
        24
    )

    var currentLevel by remember {
        mutableIntStateOf(0)
    }

    val numberCount = numbersPerLevel[currentLevel]

    /*
     * BASE VALID PATH
     *
     * This visits every one of the 36 cells exactly once.
     */
    val basePath = listOf(
        Pair(0, 0), Pair(0, 1), Pair(0, 2),
        Pair(0, 3), Pair(0, 4), Pair(0, 5),

        Pair(1, 5), Pair(1, 4), Pair(1, 3),
        Pair(1, 2), Pair(1, 1), Pair(1, 0),

        Pair(2, 0), Pair(2, 1), Pair(2, 2),
        Pair(2, 3), Pair(2, 4), Pair(2, 5),

        Pair(3, 5), Pair(3, 4), Pair(3, 3),
        Pair(3, 2), Pair(3, 1), Pair(3, 0),

        Pair(4, 0), Pair(4, 1), Pair(4, 2),
        Pair(4, 3), Pair(4, 4), Pair(4, 5),

        Pair(5, 5), Pair(5, 4), Pair(5, 3),
        Pair(5, 2), Pair(5, 1), Pair(5, 0)
    )

    /*
     * Create different VALID paths by transforming
     * the same Hamiltonian path.
     *
     * Every transformation still contains all 36 cells
     * exactly once.
     */
    fun horizontalFlip(
        path: List<Pair<Int, Int>>
    ): List<Pair<Int, Int>> {

        return path.map {
            Pair(it.first, gridSize - 1 - it.second)
        }
    }

    fun verticalFlip(
        path: List<Pair<Int, Int>>
    ): List<Pair<Int, Int>> {

        return path.map {
            Pair(gridSize - 1 - it.first, it.second)
        }
    }

    fun transpose(
        path: List<Pair<Int, Int>>
    ): List<Pair<Int, Int>> {

        return path.map {
            Pair(it.second, it.first)
        }
    }

    val levelPaths = listOf(

        // LEVEL 1
        basePath,

        // LEVEL 2
        basePath.reversed(),

        // LEVEL 3
        horizontalFlip(basePath),

        // LEVEL 4
        horizontalFlip(basePath).reversed(),

        // LEVEL 5
        verticalFlip(basePath),

        // LEVEL 6
        verticalFlip(basePath).reversed(),

        // LEVEL 7
        transpose(basePath),

        // LEVEL 8
        transpose(basePath).reversed()
    )

    val currentPath = levelPaths[currentLevel]

    /*
     * Randomly choose positions for the numbered points.
     *
     * First number is always at the starting cell.
     * Last number is always at the ending cell.
     *
     * The other numbers are placed at random positions
     * along the valid path.
     */
    val currentNumbers = remember(currentLevel) {

        val indexes = mutableListOf<Int>()

        indexes.add(0)

        if (numberCount > 2) {

            val middleIndexes =
                (1 until totalCells - 1).shuffled()

            indexes.addAll(
                middleIndexes.take(numberCount - 2)
            )
        }

        indexes.add(totalCells - 1)

        val sortedIndexes =
            indexes.sorted()

        val result =
            mutableMapOf<Int, Pair<Int, Int>>()

        for (number in 1..numberCount) {

            result[number] =
                currentPath[
                    sortedIndexes[number - 1]
                ]
        }

        result
    }

    var path by remember(currentLevel) {
        mutableStateOf(
            listOf(currentPath[0])
        )
    }

    var nextNumber by remember(currentLevel) {
        mutableIntStateOf(2)
    }

    var completed by remember(currentLevel) {
        mutableStateOf(false)
    }

    var showHint by remember(currentLevel) {
        mutableStateOf(false)
    }

    /*
     * Move to a grid cell.
     */
    fun moveToCell(
        row: Int,
        column: Int
    ) {

        if (completed) return

        if (row !in 0 until gridSize) return

        if (column !in 0 until gridSize) return

        val newCell =
            Pair(row, column)

        val lastCell =
            path.last()

        val rowDifference =
            abs(lastCell.first - row)

        val columnDifference =
            abs(lastCell.second - column)

        // Only adjacent cells are allowed
        if (
            rowDifference +
            columnDifference != 1
        ) {
            return
        }

        // A cell cannot be used twice
        if (newCell in path) {
            return
        }

        /*
         * The player must follow the hidden
         * valid path.
         */
        val expectedCell =
            currentPath[path.size]

        if (newCell != expectedCell) {
            return
        }

        path = path + newCell

        val numberAtCell =
            currentNumbers.entries
                .firstOrNull {
                    it.value == newCell
                }
                ?.key

        if (numberAtCell == nextNumber) {
            nextNumber++
        }

        showHint = false

        /*
         * The level is complete only when
         * every cell has been connected and
         * every number has been reached.
         */
        if (
            path.size == totalCells &&
            nextNumber > numberCount
        ) {

            completed = true
        }
    }

    /*
     * Reset current level.
     */
    fun resetGame() {

        path =
            listOf(currentPath[0])

        nextNumber = 2

        completed = false

        showHint = false
    }

    /*
     * MAIN SCREEN
     */
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(18.dp),

        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        /*
         * GAME TITLE
         */
        Text(
            text = "ZIP",
            fontSize = 42.sp,
            color = Color(0xFF5B4BDB)
        )

        Text(
            text = "Connect the numbers",
            fontSize = 17.sp,
            color = Color(0xFF555555)
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        /*
         * LEVEL INFORMATION
         */
        Row(
            modifier =
                Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.SpaceBetween,

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Text(
                text =
                    "LEVEL ${currentLevel + 1}",

                fontSize = 20.sp,

                color =
                    Color(0xFF5B4BDB)
            )

            Text(
                text =
                    "$numberCount NUMBERS",

                fontSize = 15.sp,

                color =
                    Color(0xFF777777)
            )
        }

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        /*
         * GAME GRID
         */
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(
                    Color(0xFFF1F2F6),
                    RoundedCornerShape(18.dp)
                )
                .padding(5.dp)

                /*
                 * DRAG CONTROL
                 */
                .pointerInput(currentLevel) {

                    detectDragGestures(

                        onDrag = { change, _ ->

                            val cellWidth =
                                size.width.toFloat() /
                                        gridSize

                            val cellHeight =
                                size.height.toFloat() /
                                        gridSize

                            val column =
                                (
                                        change.position.x /
                                                cellWidth
                                        ).toInt()

                            val row =
                                (
                                        change.position.y /
                                                cellHeight
                                        ).toInt()

                            moveToCell(
                                row,
                                column
                            )
                        }
                    )
                }
        ) {

            Column(
                modifier =
                    Modifier.fillMaxSize(),

                verticalArrangement =
                    Arrangement.spacedBy(3.dp)
            ) {

                for (
                row in 0 until gridSize
                ) {

                    Row(
                        modifier =
                            Modifier.weight(1f),

                        horizontalArrangement =
                            Arrangement.spacedBy(3.dp)
                    ) {

                        for (
                        column in 0 until gridSize
                        ) {

                            val cell =
                                Pair(row, column)

                            val isPath =
                                cell in path

                            val number =
                                currentNumbers.entries
                                    .firstOrNull {
                                        it.value == cell
                                    }
                                    ?.key

                            val isHint =
                                showHint &&
                                        number ==
                                        nextNumber

                            /*
                             * CELL
                             */
                            Box(
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .background(

                                            when {

                                                isPath ->
                                                    Color(
                                                        0xFF6C63FF
                                                    )

                                                isHint ->
                                                    Color(
                                                        0xFFFFC107
                                                    )

                                                else ->
                                                    Color(
                                                        0xFFE9EAF0
                                                    )
                                            },

                                            RoundedCornerShape(
                                                8.dp
                                            )
                                        ),

                                contentAlignment =
                                    Alignment.Center
                            ) {

                                /*
                                 * NUMBER CIRCLE
                                 */
                                if (
                                    number != null
                                ) {

                                    Box(
                                        modifier =
                                            Modifier
                                                .size(
                                                    when {

                                                        numberCount >= 18 ->
                                                            34.dp

                                                        numberCount >= 12 ->
                                                            38.dp

                                                        else ->
                                                            43.dp
                                                    }
                                                )
                                                .background(
                                                    Color.White,
                                                    CircleShape
                                                ),

                                        contentAlignment =
                                            Alignment.Center
                                    ) {

                                        Text(
                                            text =
                                                number.toString(),

                                            fontSize =
                                                when {

                                                    numberCount >= 18 ->
                                                        12.sp

                                                    numberCount >= 12 ->
                                                        14.sp

                                                    else ->
                                                        16.sp
                                                },

                                            color =
                                                Color(0xFF222222)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        /*
         * COMPLETION SCREEN
         */
        if (completed) {

            Text(
                text =
                    "🎉 Level Complete!",

                fontSize = 22.sp,

                color =
                    Color(0xFF22A06B)
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            if (
                currentLevel <
                numbersPerLevel.lastIndex
            ) {

                Button(
                    onClick = {

                        currentLevel++
                    },

                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                Color(0xFF5B4BDB)
                        )
                ) {

                    Text(
                        text =
                            "Next Level"
                    )
                }

            } else {

                Text(
                    text =
                        "🏆 All Levels Complete!",

                    fontSize = 19.sp,

                    color =
                        Color(0xFF333333)
                )
            }

        } else {

            /*
             * GAME INFORMATION
             */
            Text(
                text =
                    "Next: $nextNumber",

                fontSize = 21.sp,

                color =
                    Color(0xFF333333)
            )

            Text(
                text =
                    "Progress: ${path.size}/$totalCells",

                fontSize = 15.sp,

                color =
                    Color(0xFF777777)
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            /*
             * BUTTONS
             */
            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {

                Button(
                    onClick = {

                        showHint = true
                    },

                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                Color(0xFFFFA000)
                        )
                ) {

                    Text(
                        text =
                            "💡 Hint"
                    )
                }

                Button(
                    onClick = {

                        resetGame()
                    },

                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                Color(0xFF5B4BDB)
                        )
                ) {

                    Text(
                        text =
                            "Reset"
                    )
                }
            }
        }
    }
}