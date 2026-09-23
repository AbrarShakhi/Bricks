package com.abrarshakhi.bricks.core.designsystem.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

object BricksIcons {
    val Search: ImageVector by lazy {
        inkIcon("Search", "M17,10.5 A6.5,6.5 0 1,1 4,10.5 A6.5,6.5 0 1,1 17,10.5 Z M15.4,15.4 L20.5,20.5")
    }
    val Settings: ImageVector by lazy {
        inkIcon(
            "Settings",
            "M15,12 A3,3 0 1,1 9,12 A3,3 0 1,1 15,12 Z " +
                "M19,12 A7,7 0 1,1 5,12 A7,7 0 1,1 19,12 Z " +
                "M19,12 L21.5,12 M16.95,16.95 L18.72,18.72 M12,19 L12,21.5 M7.05,16.95 L5.28,18.72 " +
                "M5,12 L2.5,12 M7.05,7.05 L5.28,5.28 M12,5 L12,2.5 M16.95,7.05 L18.72,5.28",
        )
    }
    val Close: ImageVector by lazy { inkIcon("Close", "M6,6 L18,18 M18,6 L6,18") }
    val Check: ImageVector by lazy { inkIcon("Check", "M5,12.5 L10,17.5 L19,7") }
    val Plus: ImageVector by lazy { inkIcon("Plus", "M12,5 L12,19 M5,12 L19,12") }
    val Minus: ImageVector by lazy { inkIcon("Minus", "M5,12 L19,12") }
    val ArrowBack: ImageVector by lazy { inkIcon("ArrowBack", "M19,12 L5,12 M11,6 L5,12 L11,18", autoMirror = true) }
    val ArrowUp: ImageVector by lazy { inkIcon("ArrowUp", "M12,19 L12,5 M6,11 L12,5 L18,11") }
    val ChevronRight: ImageVector by lazy { inkIcon("ChevronRight", "M9,5 L16,12 L9,19", autoMirror = true) }
    val ChevronDown: ImageVector by lazy { inkIcon("ChevronDown", "M5,9 L12,16 L19,9") }
    val Info: ImageVector by lazy {
        inkIcon("Info", "M21,12 A9,9 0 1,1 3,12 A9,9 0 1,1 21,12 Z M12,11 L12,16.5 M12,7.6 L12,7.7")
    }
    val Trash: ImageVector by lazy {
        inkIcon(
            "Trash",
            "M4,7 L20,7 M9,7 L9,4.5 L15,4.5 L15,7 M6.5,7 L7.5,20 L16.5,20 L17.5,7 M10,11 L10,16 M14,11 L14,16",
        )
    }
    val Eye: ImageVector by lazy {
        inkIcon(
            "Eye",
            "M2.5,12 C5,7 8.5,5 12,5 C15.5,5 19,7 21.5,12 C19,17 15.5,19 12,19 C8.5,19 5,17 2.5,12 Z " +
                "M15,12 A3,3 0 1,1 9,12 A3,3 0 1,1 15,12 Z",
        )
    }
    val EyeOff: ImageVector by lazy {
        inkIcon(
            "EyeOff",
            "M2.5,12 C5,7 8.5,5 12,5 C15.5,5 19,7 21.5,12 C19,17 15.5,19 12,19 C8.5,19 5,17 2.5,12 Z " +
                "M15,12 A3,3 0 1,1 9,12 A3,3 0 1,1 15,12 Z M4,4 L20,20",
        )
    }
    val Edit: ImageVector by lazy {
        inkIcon("Edit", "M4,20 L4.5,15.5 L15.5,4.5 L19.5,8.5 L8.5,19.5 Z M13.5,6.5 L17.5,10.5")
    }
    val Folder: ImageVector by lazy { inkIcon("Folder", "M3,5.5 L9.5,5.5 L11.5,8 L21,8 L21,19 L3,19 Z M3,11 L21,11") }
    val Widgets: ImageVector by lazy {
        inkIcon(
            "Widgets",
            "M4,4 L10,4 L10,10 L4,10 Z M4,14 L10,14 L10,20 L4,20 Z M14,14 L20,14 L20,20 L14,20 Z M17,2.8 L21.2,7 L17,11.2 L12.8,7 Z",
        )
    }
    val Apps: ImageVector by lazy {
        inkIcon(
            "Apps",
            stroke = null,
            fill = "M4,4 h4 v4 h-4 Z M10,4 h4 v4 h-4 Z M16,4 h4 v4 h-4 Z " +
                "M4,10 h4 v4 h-4 Z M10,10 h4 v4 h-4 Z M16,10 h4 v4 h-4 Z " +
                "M4,16 h4 v4 h-4 Z M10,16 h4 v4 h-4 Z M16,16 h4 v4 h-4 Z",
        )
    }
    val Wallpaper: ImageVector by lazy {
        inkIcon(
            "Wallpaper",
            "M3,5 L21,5 L21,19 L3,19 Z M3,16 L8,11 L12,15 L15,12 L21,18 M18,8.5 A1.5,1.5 0 1,1 15,8.5 A1.5,1.5 0 1,1 18,8.5 Z",
        )
    }
    val Home: ImageVector by lazy {
        inkIcon("Home", "M3,11 L12,3.5 L21,11 M5.5,9 L5.5,20 L18.5,20 L18.5,9 M10,20 L10,14 L14,14 L14,20")
    }
    val Lock: ImageVector by lazy {
        inkIcon("Lock", "M5.5,11 L18.5,11 L18.5,20.5 L5.5,20.5 Z M8.5,11 L8.5,8 A3.5,3.5 0 0,1 15.5,8 L15.5,11 M12,14.5 L12,17")
    }
    val Bell: ImageVector by lazy {
        inkIcon("Bell", "M6,16 L6,11 A6,6 0 0,1 18,11 L18,16 L19.5,18 L4.5,18 Z M10,20.5 A2,2 0 0,0 14,20.5")
    }
    val Palette: ImageVector by lazy {
        inkIcon(
            "Palette",
            "M12,3 A9,9 0 1,0 12,21 C13.5,21 14,20 13.5,18.8 C13,17.5 13.8,16.5 15.2,16.5 L17,16.5 A4,4 0 0,0 21,12.5 C21,7.3 17,3 12,3 Z " +
                "M7.5,11 L7.5,11.1 M10.5,7.5 L10.5,7.6 M15,7.5 L15,7.6",
        )
    }
    val Gestures: ImageVector by lazy {
        inkIcon("Gestures", "M8,19 L8,5 M4.5,8.5 L8,5 L11.5,8.5 M16,5 L16,19 M12.5,15.5 L16,19 L19.5,15.5")
    }
    val Dock: ImageVector by lazy {
        inkIcon("Dock", "M3,16 L21,16 L21,20.5 L3,20.5 Z M5.5,5 L10.5,5 L10.5,10 L5.5,10 Z M13.5,5 L18.5,5 L18.5,10 L13.5,10 Z")
    }
    val Grid: ImageVector by lazy {
        inkIcon("Grid", "M4,4 L20,4 L20,20 L4,20 Z M9.33,4 L9.33,20 M14.66,4 L14.66,20 M4,9.33 L20,9.33 M4,14.66 L20,14.66")
    }
    val Sort: ImageVector by lazy {
        inkIcon("Sort", "M4,7 L13,7 M4,12 L10,12 M4,17 L7.5,17 M18,5 L18,19 M15,16 L18,19 L21,16")
    }
    val Clock: ImageVector by lazy {
        inkIcon("Clock", "M21,12 A9,9 0 1,1 3,12 A9,9 0 1,1 21,12 Z M12,7 L12,12 L15.5,14")
    }
    val Alarm: ImageVector by lazy {
        inkIcon(
            "Alarm",
            "M19,13 A7,7 0 1,1 5,13 A7,7 0 1,1 19,13 Z M12,9.5 L12,13 L14.5,14.5 M4.5,3.5 L2.5,5.5 M19.5,3.5 L21.5,5.5",
        )
    }
    val Battery: ImageVector by lazy {
        inkIcon("Battery", "M3.5,7.5 L17.5,7.5 L17.5,16.5 L3.5,16.5 Z M17.5,10.5 L20.5,10.5 L20.5,13.5 L17.5,13.5")
    }
    val Bolt: ImageVector by lazy { inkIcon("Bolt", "M13,3 L6,13 L11.5,13 L10.5,21 L18,10.5 L12.5,10.5 Z") }
    val Globe: ImageVector by lazy {
        inkIcon(
            "Globe",
            "M21,12 A9,9 0 1,1 3,12 A9,9 0 1,1 21,12 Z M3,12 L21,12 M12,3 C15,6 15,18 12,21 C9,18 9,6 12,3 Z",
        )
    }
    val Store: ImageVector by lazy {
        inkIcon("Store", "M5,8 L19,8 L18,20.5 L6,20.5 Z M9,8 L9,6.5 A3,3 0 0,1 15,6.5 L15,8")
    }
    val Calculator: ImageVector by lazy {
        inkIcon(
            "Calculator",
            "M6,3 L18,3 L18,21 L6,21 Z M8.5,6 L15.5,6 L15.5,9 L8.5,9 Z M9,13 L10,13 M14,13 L15,13 M9,17 L10,17 M14,17 L15,17",
        )
    }
    val Pin: ImageVector by lazy {
        inkIcon("Pin", "M15,3 L21,9 L18,10 L14.5,13.5 L14,18 L6,10 L10.5,9.5 L14,6 Z M9.5,14.5 L4,20")
    }
    val More: ImageVector by lazy {
        inkIcon("More", "M12,5 L12,5.1 M12,12 L12,12.1 M12,19 L12,19.1", strokeWidth = 3.4f)
    }
    val Work: ImageVector by lazy {
        inkIcon("Work", "M3.5,8 L20.5,8 L20.5,19.5 L3.5,19.5 Z M9,8 L9,5.5 L15,5.5 L15,8 M3.5,13 L20.5,13")
    }
    val Pause: ImageVector by lazy { inkIcon("Pause", "M9,6 L9,18 M15,6 L15,18") }
    val Play: ImageVector by lazy { inkIcon("Play", "M8,5 L19,12 L8,19 Z") }
    val Refresh: ImageVector by lazy { inkIcon("Refresh", "M20,12 A8,8 0 1,1 17.6,6.3 M20,4 L20,9 L15,9") }
    val Label: ImageVector by lazy { inkIcon("Label", "M3,12 L3,4 L11,4 L20,13 L13,20 Z M7.5,7.5 L7.5,7.6") }
    val Star: ImageVector by lazy {
        inkIcon(
            "Star",
            stroke = null,
            fill = "M12,2 L15.1,8.6 L22,9.5 L17,14.6 L18.2,22 L12,18.5 L5.8,22 L7,14.6 L2,9.5 L8.9,8.6 Z",
        )
    }
    val PageAdd: ImageVector by lazy {
        inkIcon("PageAdd", "M6,3 L14,3 L19,8 L19,21 L6,21 Z M14,3 L14,8 L19,8 M12.5,11 L12.5,17 M9.5,14 L15.5,14")
    }
    val Sparkle: ImageVector by lazy {
        inkIcon(
            "Sparkle",
            "M11,3 L12.8,8.7 L18.5,10.5 L12.8,12.3 L11,18 L9.2,12.3 L3.5,10.5 L9.2,8.7 Z " +
                "M18.5,15.5 L19.3,17.7 L21.5,18.5 L19.3,19.3 L18.5,21.5 L17.7,19.3 L15.5,18.5 L17.7,17.7 Z",
        )
    }
    val Resize: ImageVector by lazy {
        inkIcon("Resize", "M14,4 L20,4 L20,10 M10,20 L4,20 L4,14 M20,4 L13,11 M4,20 L11,13")
    }
    val Sun: ImageVector by lazy {
        inkIcon(
            "Sun",
            "M16,12 A4,4 0 1,1 8,12 A4,4 0 1,1 16,12 Z M12,2.5 L12,4.5 M12,19.5 L12,21.5 M2.5,12 L4.5,12 M19.5,12 L21.5,12 " +
                "M5.3,5.3 L6.7,6.7 M17.3,17.3 L18.7,18.7 M5.3,18.7 L6.7,17.3 M17.3,6.7 L18.7,5.3",
        )
    }
    val Moon: ImageVector by lazy { inkIcon("Moon", "M20,14.5 A8.5,8.5 0 1,1 9.5,4 A7,7 0 0,0 20,14.5 Z") }
    val Layers: ImageVector by lazy {
        inkIcon("Layers", "M12,3 L21,8 L12,13 L3,8 Z M3,12.5 L12,17.5 L21,12.5 M3,16.5 L12,21.5 L21,16.5")
    }
    val ListView: ImageVector by lazy {
        inkIcon("ListView", "M9,6 L20,6 M9,12 L20,12 M9,18 L20,18 M4.5,6 L4.5,6.1 M4.5,12 L4.5,12.1 M4.5,18 L4.5,18.1")
    }
    val Letter: ImageVector by lazy { inkIcon("Letter", "M5,20 L12,4 L19,20 M8,14 L16,14") }
    val Copy: ImageVector by lazy { inkIcon("Copy", "M8,8 L20,8 L20,20 L8,20 Z M16,8 L16,4 L4,4 L4,16 L8,16") }
    val Texture: ImageVector by lazy {
        inkIcon(
            "Texture",
            "M5,5 L5,5.1 M10,5 L10,5.1 M15,5 L15,5.1 M20,5 L20,5.1 M7.5,9.5 L7.5,9.6 M12.5,9.5 L12.5,9.6 M17.5,9.5 L17.5,9.6 " +
                "M5,14 L5,14.1 M10,14 L10,14.1 M15,14 L15,14.1 M20,14 L20,14.1 M7.5,18.5 L7.5,18.6 M12.5,18.5 L12.5,18.6 M17.5,18.5 L17.5,18.6",
            strokeWidth = 2.8f,
        )
    }
    val DragHandle: ImageVector by lazy {
        inkIcon(
            "DragHandle",
            "M9,6 L9,6.1 M15,6 L15,6.1 M9,12 L9,12.1 M15,12 L15,12.1 M9,18 L9,18.1 M15,18 L15,18.1",
            strokeWidth = 3.2f,
        )
    }
    val Shield: ImageVector by lazy { inkIcon("Shield", "M12,3 L20,6 L20,11.5 C20,16 16.5,19.5 12,21 C7.5,19.5 4,16 4,11.5 L4,6 Z") }
    val Undo: ImageVector by lazy { inkIcon("Undo", "M9,14 L4,9 L9,4 M4,9 L14.5,9 A5.5,5.5 0 0,1 14.5,20 L11,20", autoMirror = true) }
}

private fun inkIcon(
    name: String,
    stroke: String?,
    fill: String? = null,
    strokeWidth: Float = 2.2f,
    autoMirror: Boolean = false,
): ImageVector = ImageVector.Builder(
    name = name,
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
    autoMirror = autoMirror,
).apply {
    if (fill != null) {
        addPath(pathData = addPathNodes(fill), fill = SolidColor(Color.Black))
    }
    if (stroke != null) {
        addPath(
            pathData = addPathNodes(stroke),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = strokeWidth,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
    }
}.build()
