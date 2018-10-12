package ru.livli.taploggertest

import android.os.*
import android.util.ArrayMap
import android.util.Log
import android.view.*
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.experimental.async
import org.jetbrains.anko.childrenRecursiveSequence
import org.jetbrains.anko.contentView
import java.lang.reflect.Field

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        TapLogger.start()
    }
}

object TapLogger {
    private const val viewStr = "android.view.View"

    fun start() {
//        val looper = Looper.getMainLooper()
//        Handler(looper) {
//            "--- $it".error
//            false
//        }

        async {
            while (true) {
                dumpQueue()
                Thread.sleep(1L)
            }
        }
    }

    private var messagesField: Field
    private var nextField: Field
    private var mainMessageQueue: MessageQueue

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