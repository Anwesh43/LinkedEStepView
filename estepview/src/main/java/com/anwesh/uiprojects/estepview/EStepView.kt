package com.anwesh.uiprojects.estepview

/**
 * Created by anweshmishra on 13/10/18.
 */

import android.app.Activity
import android.view.View
import android.view.MotionEvent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import android.content.Context

val nodes : Int = 5

fun Canvas.drawESNode(i : Int, scale : Float, paint : Paint) {
    val w : Float = width.toFloat()
    val h : Float = height.toFloat()
    val gap : Float = w / (nodes + 1)
    val size : Float = gap/3
    paint.color = Color.parseColor("#3F51B5")
    paint.strokeWidth = Math.min(w, h) / 60
    paint.strokeCap = Paint.Cap.ROUND
    save()
    translate(i * gap + gap, h/2)
    drawLine(0f, 0f, size, 0f, paint)
    for (j in 0..1) {
        val sc : Float = Math.min(0.5f, Math.max(scale - 0.5f * j, 0f)) * 2
        val sf : Float = 1f - 2 * j
        val y : Float = size * sf * sc
        save()
        drawLine(0f, y, size, 0f, paint)
        drawLine(0f, 0f, 0f, y, paint)
        restore()
    }
    restore()
}
class EStepView(ctx : Context) : View(ctx) {

    private val paint : Paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val renderer : Renderer = Renderer(this)

    override fun onDraw(canvas : Canvas) {
        renderer.render(canvas, paint)
    }

    override fun onTouchEvent(event : MotionEvent) : Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                renderer.handleTap()
            }
        }
        return true
    }

    data class State(var scale : Float = 0f, var prevScale : Float = 0f, var dir : Float = 0f) {

        fun update(cb : (Float) -> Unit) {
            scale += 0.05f * dir
            if (Math.abs(scale - prevScale) > 1) {
                scale = prevScale + dir
                dir = 0f
                prevScale = scale
                cb(prevScale)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            if (dir == 0f) {
                dir = 1f - 2 * prevScale
                cb()
            }
        }
    }

    data class Animator(var view : View, var animated : Boolean = false) {
        fun animate(cb : () -> Unit) {
            if (animated) {
                cb()
                try {
                    Thread.sleep(50)
                    view.invalidate()
                } catch(ex : Exception) {

                }
            }
        }

        fun start() {
            if (!animated) {
                animated = true
                view.postInvalidate()
            }
        }

        fun stop() {
            if (animated) {
                animated = false
            }
        }
    }

    data class EStepNode(var i : Int, val state : State = State()) {

        private var next : EStepNode? = null
        private var prev : EStepNode? = null

        init {
            addNeighbor()
        }

        fun addNeighbor() {
            if (i < nodes - 1) {
                next = EStepNode(i + 1)
                next?.prev = this
            }
        }
        fun draw(canvas : Canvas, paint : Paint) {
            canvas.drawESNode(i, state.scale, paint)
            next?.draw(canvas, paint)
        }

        fun update(cb : (Int, Float) -> Unit) {
            state.update {
                cb(i, it)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            state.startUpdating(cb)
        }

        fun getNext(dir : Int, cb : () -> Unit) : EStepNode {
            var curr : EStepNode? = prev
            if (dir == 1) {
                curr = next
            }
            if (curr != null) {
                return curr
            }
            cb()
            return this
        }
    }

    data class EStep(var i : Int) {
        private var root : EStepNode = EStepNode(0)
        private var curr : EStepNode = root
        private var dir : Int = 1

        fun draw(canvas : Canvas, paint : Paint) {
            root.draw(canvas, paint)
        }

        fun update(cb : (Int, Float) -> Unit) {
            curr.update {i, scl ->
                curr = curr.getNext(dir) {
                    dir *= -1
                }
                cb(i, scl)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            curr.startUpdating(cb)
        }
    }

    data class Renderer(var view : EStepView) {

        private val animator : Animator = Animator(view)

        private val es : EStep = EStep(0)

        fun render(canvas : Canvas, paint : Paint) {
            canvas.drawColor(Color.parseColor("#BDBDBD"))
            es.draw(canvas, paint)
            animator.animate {
                es.update {i, scl ->
                    animator.stop()
                }
            }
        }

        fun handleTap() {
            es.startUpdating {
                animator.start()
            }
        }
    }

    companion object {
        fun create(activity : Activity) : EStepView {
            val view : EStepView = EStepView(activity)
            activity.setContentView(view)
            return view 
        }
    }
}
