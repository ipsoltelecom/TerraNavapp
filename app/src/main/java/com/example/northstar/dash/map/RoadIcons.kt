package com.example.northstar.dash.map

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF

object RoadIcons {
    fun bitmap(camera: Boolean): Bitmap {
        val bitmap = Bitmap.createBitmap(48,48,Bitmap.Config.ARGB_8888)
        val c = Canvas(bitmap)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.color = Color.rgb(7,19,35); c.drawCircle(24f,24f,23f,p)
        p.style = Paint.Style.STROKE; p.strokeWidth = 2f
        p.color = Color.rgb(117,186,255); c.drawCircle(24f,24f,22f,p)
        if (camera) {
            c.drawRoundRect(RectF(11f,17f,37f,33f),3f,3f,p)
            c.drawCircle(24f,25f,6f,p)
            c.drawLine(17f,16f,21f,12f,p); c.drawLine(21f,12f,29f,12f,p)
        } else {
            c.drawRoundRect(RectF(17f,8f,31f,37f),4f,4f,p)
            p.style = Paint.Style.FILL; p.color = Color.rgb(222,232,245)
            for (y in listOf(15f,23f,31f)) c.drawCircle(24f,y,2.5f,p)
        }
        return bitmap
    }
}
