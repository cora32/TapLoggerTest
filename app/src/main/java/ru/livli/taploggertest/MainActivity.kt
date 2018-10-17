package ru.livli.taploggertest

import android.app.Activity
import android.content.Intent
import android.graphics.Rect
import android.os.*
import android.util.ArrayMap
import android.util.Log
import android.view.*
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.delay
import org.jetbrains.anko.childrenRecursiveSequence
import org.jetbrains.anko.contentView
import java.lang.ref.WeakReference
import java.lang.reflect.Field

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tv.setOnClickListener {
            "--- ORIGINAL click".error
            startActivity(Intent(this, SecondActivity::class.java))
        }

        async {
            TapLogger.start()
        }.invokeOnCompletion {
            if (it != null)
                "--- G ERROR $it".error
        }
    }
}

class SecondActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}

object TapLogger {
    private val r = Rect()
    private const val viewStr = "android.view.View"
    private var messagesField: Field
    private var nextField: Field
    private var mainMessageQueue: MessageQueue
    private var selectedView: WeakReference<View>? = null
    private var oldActivity: WeakReference<AppCompatActivity>? = null

    init {
        try {
            val queueField = Looper::class.java.getDeclaredField("mQueue")
            queueField.isAccessible = true
            messagesField = MessageQueue::class.java.getDeclaredField("mMessages")
            messagesField.isAccessible = true
            nextField = Message::class.java.getDeclaredField("next")
            nextField.isAccessible = true
            val mainLooper = Looper.getMainLooper()
            mainMessageQueue = queueField.get(mainLooper) as MessageQueue
        } catch (e: Exception) {
            throw RuntimeException(e)
        }

    }

    fun getReflectedActivity(): AppCompatActivity? {
        val activityThreadClass = Class.forName("android.app.ActivityThread")
        val activityThread = activityThreadClass.getMethod("currentActivityThread")
                .invoke(null)
        val activitiesField = activityThreadClass.getDeclaredField("mActivities")
        activitiesField.isAccessible = true
        val activities = activitiesField.get(activityThread) as ArrayMap<*, *>
        if (activities.isNotEmpty()) {
            val activityClient = activities.valueAt(0)
            activityClient?.let {
                val activityField = activityClient::class.java.getDeclaredField("activity")
                activityField.isAccessible = true
                return activityField.get(activityClient) as? AppCompatActivity
            }
        }
        return null
    }

    fun start() {
        var activity = TapLogger.getReflectedActivity()

        async {
            while (true) {
                delay(700)
                activity = getReflectedActivity()
                "--- act: ${activity?.javaClass?.name}".error
                if (activity?.javaClass?.name != oldActivity?.get()?.javaClass?.name) {
                    activity?.let { act -> oldActivity = WeakReference(act) }
                    "--- activitySwitched to ${oldActivity?.get()?.javaClass?.name}".error
                    setupListeners(activity)
                }
            }
        }

        setupListeners(activity)


//        async {
//            while (true) {
//                dumpQueue()
//                Thread.sleep(1L)
//            }
//        }
    }

    private fun setupListeners(activity: AppCompatActivity?) {
        if (activity == null)
            return

        val list = object : GestureDetector.OnGestureListener {
            override fun onShowPress(p0: MotionEvent?) {
                "--- onShowPress".error
            }

            override fun onSingleTapUp(p0: MotionEvent?): Boolean {
                "--- onSingleTapUp".error
                selectedView?.get()?.performClick()
                return false
            }

            override fun onDown(p0: MotionEvent?): Boolean {
                "--- onDown".error
                return false
            }

            override fun onFling(p0: MotionEvent?, p1: MotionEvent?, p2: Float, p3: Float): Boolean {
                if (Math.abs(p2) > Math.abs(p3)) {
                    if (p2 > 0.5f) "--- onFling right".error
                    else "--- onFling left".error
                } else {
                    if (p3 > 0.5f) "--- onFling down".error
                    else "--- onFling up".error
                }

                return false
            }

            override fun onScroll(p0: MotionEvent?, p1: MotionEvent?, p2: Float, p3: Float): Boolean {
                "--- onScroll".error
                return false
            }

            override fun onLongPress(p0: MotionEvent?) {
                "--- onLongPress".error
            }


        }
        Handler(Looper.getMainLooper()).post {
            val detector = GestureDetector(activity, list)

            val oldCallback = activity.window.callback
            activity.window?.callback = object : Window.Callback {
                override fun onActionModeFinished(mode: ActionMode?) {
                    //To change body of created functions use File | Settings | File Templates. return false
                    return oldCallback.onActionModeFinished(mode)
                }

                override fun onCreatePanelView(featureId: Int): View? {
                    //To change body of created functions use File | Settings | File Templates. return false
                    return oldCallback.onCreatePanelView(featureId)
                }

                override fun onCreatePanelMenu(featureId: Int, menu: Menu?): Boolean {
                    //To change body of created functions use File | Settings | File Templates. return false
                    return oldCallback.onCreatePanelMenu(featureId, menu)
                }

                override fun onWindowStartingActionMode(callback: ActionMode.Callback?): ActionMode? {
                    //To change body of created functions use File | Settings | File Templates. return false
                    return oldCallback.onWindowStartingActionMode(callback)
                }

                @RequiresApi(Build.VERSION_CODES.M)
                override fun onWindowStartingActionMode(callback: ActionMode.Callback?, type: Int): ActionMode? {
                    //To change body of created functions use File | Settings | File Templates. return false
                    return oldCallback.onWindowStartingActionMode(callback, type)
                }

                override fun onAttachedToWindow() {
                    "--- onAttachedToWindow".error
                    oldCallback.onAttachedToWindow()
                }

                override fun dispatchGenericMotionEvent(event: MotionEvent?): Boolean {
                    "--- dispatchGenericMotionEvent".error
                    return oldCallback.dispatchGenericMotionEvent(event)
                }

                override fun dispatchPopulateAccessibilityEvent(event: AccessibilityEvent?): Boolean {
                    //To change body of created functions use File | Settings | File Templates. return false
                    return oldCallback.dispatchPopulateAccessibilityEvent(event)
                }

                override fun dispatchTrackballEvent(event: MotionEvent?): Boolean {
                    //To change body of created functions use File | Settings | File Templates. return false
                    return oldCallback.dispatchTrackballEvent(event)
                }

                override fun dispatchKeyShortcutEvent(event: KeyEvent?): Boolean {
                    //To change body of created functions use File | Settings | File Templates. return false
                    return oldCallback.dispatchKeyShortcutEvent(event)
                }

                override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
                    //To change body of created functions use File | Settings | File Templates. return false
                    return oldCallback.dispatchKeyEvent(event)
                }

                override fun onMenuOpened(featureId: Int, menu: Menu?): Boolean {
                    //To change body of created functions use File | Settings | File Templates. return false
                    return oldCallback.onMenuOpened(featureId, menu)
                }

                override fun onPanelClosed(featureId: Int, menu: Menu?) {
                    //To change body of created functions use File | Settings | File Templates. return false
                    oldCallback.onPanelClosed(featureId, menu)
                }

                override fun onMenuItemSelected(featureId: Int, item: MenuItem?): Boolean {
                    //To change body of created functions use File | Settings | File Templates. return false
                    return oldCallback.onMenuItemSelected(featureId, item)
                }

                override fun onDetachedFromWindow() {
                    "--- onDetachedFromWindow".error
                    oldCallback.onDetachedFromWindow()
                }

                override fun onPreparePanel(featureId: Int, view: View?, menu: Menu?): Boolean {
                    //To change body of created functions use File | Settings | File Templates. return false
                    return oldCallback.onPreparePanel(featureId, view, menu)
                }

                override fun onWindowAttributesChanged(attrs: WindowManager.LayoutParams?) {
                    //To change body of created functions use File | Settings | File Templates. return false
                    oldCallback.onWindowAttributesChanged(attrs)
                }

                override fun onWindowFocusChanged(hasFocus: Boolean) {
                    //To change body of created functions use File | Settings | File Templates. return false
                    oldCallback.onWindowFocusChanged(hasFocus)
                }

                override fun onContentChanged() {
                    //To change body of created functions use File | Settings | File Templates. return false
                    return oldCallback.onContentChanged()
                }

                override fun onSearchRequested(): Boolean {
                    //To change body of created functions use File | Settings | File Templates. return false
                    return oldCallback.onSearchRequested()
                }

                @RequiresApi(Build.VERSION_CODES.M)
                override fun onSearchRequested(searchEvent: SearchEvent?): Boolean {
                    //To change body of created functions use File | Settings | File Templates. return false return false
                    return oldCallback.onSearchRequested(searchEvent)
                }

                override fun onActionModeStarted(mode: ActionMode?) {
                    oldCallback.onActionModeStarted(mode)
                }

                override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
                    when (event?.action) {
                        MotionEvent.ACTION_DOWN -> {
                            "--- dispatching".error
                            if (!iterateChildren(activity, event))
                                "--- unresponsive?".error
                        }
                    }

                    detector.onTouchEvent(event)
                    return oldCallback.dispatchTouchEvent(event)
                }

            }
        }
    }

    private fun iterateChildren(activity: Activity, event: MotionEvent): Boolean {
        activity.window?.decorView?.childrenRecursiveSequence()?.forEach {
            if (it.isClickable) {
                when (it) {
                    is TextView -> {
                        val x = event.x.toInt()
                        val y = event.y.toInt()
                        it.getGlobalVisibleRect(r)

                        if (r.contains(x, y)) {
                            ("--- \"${it.text}\" ${r.contains(x, y)} ${activity.resources.getResourceName(it.id)}").error
                            return true
                        }
                    }
                    is ImageView -> {
                        val x = event.x.toInt()
                        val y = event.y.toInt()
                        it.getGlobalVisibleRect(r)

                        if (r.contains(x, y)) {
                            //get drawable and draw it to canvas here
//                            ("--- \"${it.text}\" ${r.contains(x, y)}").error
                            selectedView = WeakReference(it)
                            return true
                        }
                    }
                    else -> {
                        "--- clicked 2: ${it.isClickable} ${it.javaClass}".error
                    }
                }
            }
        }
        return false
    }

    fun dumpQueue() {
        try {
            val nextMessage = messagesField.get(mainMessageQueue) as? Message
            dumpMessages(nextMessage)
        } catch (e: IllegalAccessException) {
            "--- exception".error
        }

    }

    @Throws(IllegalAccessException::class)
    fun dumpMessages(message: Message?) {
        if (message != null) {
            if ((message.target != null
                            && !message.target.toString().contains("internal.measurement"))
            ) {
                val handler: Handler? = message.target
                try {
                    handler?.let {
                        val t2 = handler.javaClass.getDeclaredField("this$0")
                        t2.isAccessible = true
                        val obj2 = t2.get(it)
                        try {
                            val activitiesF = obj2.javaClass.getDeclaredField("mActivities")
                            activitiesF.isAccessible = true
                            val activities = activitiesF.get(obj2) as? ArrayMap<*, *>

                            activities?.forEach {
                                val tk = it.key
                                val tv = it.value

                                tv?.let {
                                    val activf = it.javaClass.getDeclaredField("activity")
                                    activf.isAccessible = true
                                    val activ = activf.get(it) as? AppCompatActivity
                                    activ?.window?.callback = object : Window.Callback {
                                        override fun onActionModeFinished(mode: ActionMode?) {
                                            //To change body of created functions use File | Settings | File Templates. return false
                                        }

                                        override fun onCreatePanelView(featureId: Int): View? {
                                            //To change body of created functions use File | Settings | File Templates. return false
                                            return null
                                        }

                                        override fun onCreatePanelMenu(featureId: Int, menu: Menu?): Boolean {
                                            //To change body of created functions use File | Settings | File Templates. return false
                                            return false
                                        }

                                        override fun onWindowStartingActionMode(callback: ActionMode.Callback?): ActionMode? {
                                            //To change body of created functions use File | Settings | File Templates. return false
                                            return null
                                        }

                                        override fun onWindowStartingActionMode(callback: ActionMode.Callback?, type: Int): ActionMode? {
                                            //To change body of created functions use File | Settings | File Templates. return false
                                            return null
                                        }

                                        override fun onAttachedToWindow() {
                                            //To change body of created functions use File | Settings | File Templates. return false
                                        }

                                        override fun dispatchGenericMotionEvent(event: MotionEvent?): Boolean {
                                            //To change body of created functions use File | Settings | File Templates. return false
                                            return false
                                        }

                                        override fun dispatchPopulateAccessibilityEvent(event: AccessibilityEvent?): Boolean {
                                            //To change body of created functions use File | Settings | File Templates. return false
                                            return false
                                        }

                                        override fun dispatchTrackballEvent(event: MotionEvent?): Boolean {
                                            //To change body of created functions use File | Settings | File Templates. return false
                                            return false
                                        }

                                        override fun dispatchKeyShortcutEvent(event: KeyEvent?): Boolean {
                                            //To change body of created functions use File | Settings | File Templates. return false
                                            return false
                                        }

                                        override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
                                            //To change body of created functions use File | Settings | File Templates. return false
                                            return false
                                        }

                                        override fun onMenuOpened(featureId: Int, menu: Menu?): Boolean {
                                            //To change body of created functions use File | Settings | File Templates. return false
                                            return false
                                        }

                                        override fun onPanelClosed(featureId: Int, menu: Menu?) {
                                            //To change body of created functions use File | Settings | File Templates. return false
                                        }

                                        override fun onMenuItemSelected(featureId: Int, item: MenuItem?): Boolean {
                                            //To change body of created functions use File | Settings | File Templates. return false
                                            return false
                                        }

                                        override fun onDetachedFromWindow() {
                                            //To change body of created functions use File | Settings | File Templates. return false
                                        }

                                        override fun onPreparePanel(featureId: Int, view: View?, menu: Menu?): Boolean {
                                            //To change body of created functions use File | Settings | File Templates. return false
                                            return false
                                        }

                                        override fun onWindowAttributesChanged(attrs: WindowManager.LayoutParams?) {
                                            //To change body of created functions use File | Settings | File Templates. return false
                                        }

                                        override fun onWindowFocusChanged(hasFocus: Boolean) {
                                            //To change body of created functions use File | Settings | File Templates. return false
                                        }

                                        override fun onContentChanged() {
                                            //To change body of created functions use File | Settings | File Templates. return false
                                        }

                                        override fun onSearchRequested(): Boolean {
                                            //To change body of created functions use File | Settings | File Templates. return false
                                            return false
                                        }

                                        override fun onSearchRequested(searchEvent: SearchEvent?): Boolean {
                                            //To change body of created functions use File | Settings | File Templates. return false return false
                                            return false
                                        }

                                        override fun onActionModeStarted(mode: ActionMode?) {

                                        }

                                        override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
                                            "--- dispatching $event".error
                                            return false
                                        }

                                    }
//                                    val view = activ?.contentView
//
//                                    view?.childrenRecursiveSequence()?.forEach {
//                                        val listener = try {
//                                            if (it.hasOnClickListeners()) {
//                                                val listenerField: Field = Class.forName(viewStr).getDeclaredField("mListenerInfo")
////                                        val listenerField = it.javaClass.getDeclaredField("mListenerInfo")
//                                                listenerField.isAccessible = true
//                                                val listenerInfo = listenerField.get(it)
//
//                                                val clickListenerField = listenerInfo.javaClass.getDeclaredField("mOnClickListener")
//                                                clickListenerField.isAccessible = true
//                                                clickListenerField.get(listenerInfo) as? View.OnClickListener
//                                            } else null
//                                        } catch (e: Exception) {
//                                            "--- exception 7".error
//                                            null
//                                        }
//
//                                        Handler(Looper.getMainLooper()).post {
//                                            it.setOnClickListener {
//                                                "--- clicked $it ${it.id} ${it.labelFor} ${(it as? Button)?.text}".error
//                                                listener?.onClick(it)
//                                            }
//                                        }
//                                    }

                                    "--- success $activ".error
                                }
                            }
                        } catch (e: Exception) {
                            "--- exception 1".error
                        }

                        try {
                            val activf = obj2.javaClass.getDeclaredField("mContext")
                            activf.isAccessible = true
                            val activ = activf.get(obj2) as? AppCompatActivity
                            val view = activ?.contentView
                            view?.childrenRecursiveSequence()?.forEach {
                                val listener = try {
                                    if (it.hasOnClickListeners()) {
                                        val listenerField: Field = Class.forName(viewStr).getDeclaredField("mListenerInfo")
//                                        val listenerField = it.javaClass.getDeclaredField("mListenerInfo")
                                        listenerField.isAccessible = true
                                        val listenerInfo = listenerField.get(it)

                                        val clickListenerField = listenerInfo.javaClass.getDeclaredField("mOnClickListener")
                                        clickListenerField.isAccessible = true
                                        clickListenerField.get(listenerInfo) as? View.OnClickListener
                                    } else null
                                } catch (e: Exception) {
                                    "--- exception 4".error
                                    null
                                }

                                it.setOnClickListener {
                                    "--- clicked $it ${it.id} ${it.labelFor} ${(it as? Button)?.text}".error
                                    listener?.onClick(it)
                                }
                            }
                            "--- success 2 $activ $view".error
                        } catch (e: Exception) {
                            "--- exception 3".error
                        }
                    }
                } catch (e: Exception) {
                    "--- exception 2".error
                }
            }

            val next = nextField.get(message) as? Message
            dumpMessages(next)
        }
    }

}

val String.error: String
    get() {
        Log.e("---", this)
        return this
    }
