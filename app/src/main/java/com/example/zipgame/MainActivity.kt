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

    /*
     * Every level has a valid path through all 36 cells.
     * Numbers are placed along that path in correct order.
     */

    val levelPaths = listOf(

        // LEVEL 1 - horizontal snake
        listOf(
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
        ),

        // LEVEL 2 - vertical snake
        listOf(
            Pair(0, 0), Pair(1, 0), Pair(2, 0),
            Pair(3, 0), Pair(4, 0), Pair(5, 0),
            Pair(5, 1), Pair(4, 1), Pair(3, 1),
            Pair(2, 1), Pair(1, 1), Pair(0, 1),
            Pair(0, 2), Pair(1, 2), Pair(2, 2),
            Pair(3, 2), Pair(4, 2), Pair(5, 2),
            Pair(5, 3), Pair(4, 3), Pair(3, 3),
            Pair(2, 3), Pair(1, 3), Pair(0, 3),
            Pair(0, 4), Pair(1, 4), Pair(2, 4),
            Pair(3, 4), Pair(4, 4), Pair(5, 4),
            Pair(5, 5), Pair(4, 5), Pair(3, 5),
            Pair(2, 5), Pair(1, 5), Pair(0, 5)
        ),

        // LEVEL 3 - reverse horizontal snake
        listOf(
            Pair(5, 5), Pair(5, 4), Pair(5, 3),
            Pair(5, 2), Pair(5, 1), Pair(5, 0),
            Pair(4, 0), Pair(4, 1), Pair(4, 2),
            Pair(4, 3), Pair(4, 4), Pair(4, 5),
            Pair(3, 5), Pair(3, 4), Pair(3, 3),
            Pair(3, 2), Pair(3, 1), Pair(3, 0),
            Pair(2, 0), Pair(2, 1), Pair(2, 2),
            Pair(2, 3), Pair(2, 4), Pair(2, 5),
            Pair(1, 5), Pair(1, 4), Pair(1, 3),
            Pair(1, 2), Pair(1, 1), Pair(1, 0),
            Pair(0, 0), Pair(0, 1), Pair(0, 2),
            Pair(0, 3), Pair(0, 4), Pair(0, 5)
        ),

        // LEVEL 4 - reverse vertical snake
        listOf(
            Pair(5, 5), Pair(4, 5), Pair(3, 5),
            Pair(2, 5), Pair(1, 5), Pair(0, 5),
            Pair(0, 4), Pair(1, 4), Pair(2, 4),
            Pair(3, 4), Pair(4, 4), Pair(5, 4),
            Pair(5, 3), Pair(4, 3), Pair(3, 3),
            Pair(2, 3), Pair(1, 3), Pair(0, 3),
            Pair(0, 2), Pair(1, 2), Pair(2, 2),
            Pair(3, 2), Pair(4, 2), Pair(5, 2),
            Pair(5, 1), Pair(4, 1), Pair(3, 1),
            Pair(2, 1), Pair(1, 1), Pair(0, 1),
            Pair(0, 0), Pair(1, 0), Pair(2, 0),
            Pair(3, 0), Pair(4, 0), Pair(5, 0)
        ),

        // LEVEL 5 - another horizontal snake
        listOf(
            Pair(2, 0), Pair(2, 1), Pair(2, 2),
            Pair(2, 3), Pair(2, 4), Pair(2, 5),
            Pair(1, 5), Pair(1, 4), Pair(1, 3),
            Pair(1, 2), Pair(1, 1), Pair(1, 0),
            Pair(0, 0), Pair(0, 1), Pair(0, 2),
            Pair(0, 3), Pair(0, 4), Pair(0, 5),
            Pair(3, 5), Pair(3, 4), Pair(3, 3),
            Pair(3, 2), Pair(3, 1), Pair(3, 0),
            Pair(4, 0), Pair(4, 1), Pair(4, 2),
            Pair(4, 3), Pair(4, 4), Pair(4, 5),
            Pair(5, 5), Pair(5, 4), Pair(5, 3),
            Pair(5, 2), Pair(5, 1), Pair(5, 0)
        )
    )

    /*
     * Put 10 numbers on each valid path.
     */
    val numbers = levelPaths.map { path ->

        mapOf(
            1 to path[0],
            2 to path[4],
            3 to path[8],
            4 to path[12],
            5 to path[16],
            6 to path[20],
            7 to path[24],
            8 to path[28],
            9 to path[32],
            10 to path[35]
        )
    }

    var currentLevel by remember {
        mutableIntStateOf(0)
    }

    val currentPath = levelPaths[currentLevel]
    val currentNumbers = numbers[currentLevel]

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

    fun moveToCell(row: Int, column: Int) {

        if (completed) return

        if (row !in 0 until gridSize) return

        if (column !in 0 until gridSize) return

        val newCell = Pair(row, column)

        val lastCell = path.last()

        val rowDifference =
            abs(lastCell.first - row)

        val columnDifference =
            abs(lastCell.second - column)

        // Only adjacent cells
        if (rowDifference + columnDifference != 1) {
            return
        }

        // Cannot use a cell twice
        if (newCell in path) {
            return
        }

        /*
         * The next cell must follow the valid
         * path for this level.
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

        if (
            path.size == totalCells &&
            nextNumber > 10
        ) {
            completed = true
        }
    }

    fun resetGame() {

        path = listOf(currentPath[0])
        nextNumber = 2
        completed = false
        showHint = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Color(0xFFF5F5FA)
            )
            .padding(20.dp),

        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Text(
            text = "ZIP",
            fontSize = 40.sp
        )

        Text(
            text = "Level ${currentLevel + 1}",
            fontSize = 22.sp
        )

        Spacer(
            modifier = Modifier.height(5.dp)
        )

        Text(
            text = "Connect the numbers",
            fontSize = 17.sp
        )

        Spacer(
            modifier = Modifier.height(15.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(
                    Color(0xFFE0E0E0),
                    RoundedCornerShape(16.dp)
                )
                .padding(3.dp)
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
                modifier = Modifier.fillMaxSize(),

                verticalArrangement =
                    Arrangement.spacedBy(2.dp)
            ) {

                for (row in 0 until gridSize) {

                    Row(
                        modifier =
                            Modifier.weight(1f),

                        horizontalArrangement =
                            Arrangement.spacedBy(2.dp)
                    ) {

                        for (column in 0 until gridSize) {

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
                                        number == nextNumber

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(
                                        when {
                                            isPath ->
                                                Color(0xFF7C4DFF)

                                            isHint ->
                                                Color(0xFFFFC107)

                                            else ->
                                                Color.White
                                        }
                                    ),

                                contentAlignment =
                                    Alignment.Center
                            ) {

                                if (number != null) {

                                    Box(
                                        modifier =
                                            Modifier
                                                .size(45.dp)
                                                .background(
                                                    Color.Black,
                                                    CircleShape
                                                ),

                                        contentAlignment =
                                            Alignment.Center
                                    ) {

                                        Text(
                                            text =
                                                number.toString(),

                                            color =
                                                Color.White,

                                            fontSize =
                                                18.sp
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
            modifier = Modifier.height(15.dp)
        )

        if (completed) {

            Text(
                text = "🎉 Level Complete!",
                fontSize = 22.sp
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            if (currentLevel < levelPaths.size - 1) {

                Button(
                    onClick = {
                        currentLevel++
                    }
                ) {

                    Text(
                        text = "Next Level"
                    )
                }

            } else {

                Text(
                    text = "🏆 All Levels Complete!",
                    fontSize = 20.sp
                )
            }

        } else {

            Text(
                text = "Next: $nextNumber",
                fontSize = 20.sp
            )

            Text(
                text =
                    "Cells: ${path.size}/$totalCells",
                fontSize = 16.sp
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {

                Button(
                    onClick = {
                        showHint = true
                    }
                ) {

                    Text(
                        text = "💡 Hint"
                    )
                }

                Button(
                    onClick = {
                        resetGame()
                    }
                ) {

                    Text(
                        text = "Reset"
                    )
                }
            }
        }
    }
}