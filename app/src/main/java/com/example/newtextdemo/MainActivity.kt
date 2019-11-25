package com.example.newtextdemo

import android.graphics.Color
import android.graphics.Point
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.View
import android.widget.TextView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.example.newtextdemo.face.QDQQFaceView
import com.example.newtextdemo.face.QMUIQQFaceView2
import com.example.newtextdemo.face.SelectableHelper
import com.qmuiteam.qmui.QMUILog
import com.qmuiteam.qmui.span.QMUITouchableSpan
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val list = mutableListOf<String>()
//        list.add("\uD83D\uDE01")
        list.add("\n\n\n")
//        list.add("\uD83D\uDE01\uD83D\uDE01\uD83D\uDE1E\uD83D\uDE37\uD83D\uDE1E\uD83D\uDE1A\uD83D\uDE1A\uD83D\uDE04\uD83D\uDE1A\uD83D\uDE13\uD83D\uDE13\uD83D\uDE01\uD83D\uDE01\uD83D\uDE1E\uD83D\uDE37\uD83D\uDE1E\uD83D\uDE1A\uD83D\uDE1A\uD83D\uDE04\uD83D\uDE1A\uD83D\uDE13\uD83D\uDE13\uD83D\uDE01\uD83D\uDE01\uD83D\uDE1E\uD83D\uDE37\uD83D\uDE1E\uD83D\uDE1A\uD83D\uDE1A\uD83D\uDE04\uD83D\uDE1A\uD83D\uDE13\uD83D\uDE13\uD83D\uDE01\uD83D\uDE01\uD83D\uDE1E\uD83D\uDE37\uD83D\uDE1E\uD83D\uDE1A\uD83D\uDE1A\uD83D\uDE04\uD83D\uDE1A\uD83D\uDE13\uD83D\uDE13\uD83D\uDE01\uD83D\uDE01\uD83D\uDE1E\uD83D\uDE37\uD83D\uDE1E\uD83D\uDE1A\uD83D\uDE1A\uD83D\uDE04\uD83D\uDE1A\uD83D\uDE13\uD83D\uDE13\uD83D\uDE01\uD83D\uDE01\uD83D\uDE1E\uD83D\uDE37\uD83D\uDE1E\uD83D\uDE1A\uD83D\uDE1A\uD83D\uDE04\uD83D\uDE1A\uD83D\uDE13\uD83D\uDE13\uD83D\uDE01\uD83D\uDE01\uD83D\uDE1E\uD83D\uDE37\uD83D\uDE1E\uD83D\uDE1A\uD83D\uDE1A\uD83D\uDE04\uD83D\uDE1A\uD83D\uDE13\uD83D\uDE13\uD83D\uDE01\uD83D\uDE01\uD83D\uDE1E\uD83D\uDE37\uD83D\uDE1E\uD83D\uDE1A\uD83D\uDE1A\uD83D\uDE04\uD83D\uDE1A\uD83D\uDE13\uD83D\uDE13\uD83D\uDE01\uD83D\uDE01\uD83D\uDE1E\uD83D\uDE37\uD83D\uDE1E\uD83D\uDE1A\uD83D\uDE1A\uD83D\uDE04\uD83D\uDE1A\uD83D\uDE13\uD83D\uDE13\uD83D\uDE01\uD83D\uDE01\uD83D\uDE1E\uD83D\uDE37\uD83D\uDE1E\uD83D\uDE1A\uD83D\uDE1A\uD83D\uDE04\uD83D\uDE1A\uD83D\uDE13\uD83D\uDE13\uD83D\uDE01\uD83D\uDE01\uD83D\uDE1E\uD83D\uDE37\uD83D\uDE1E\uD83D\uDE1A\uD83D\uDE1A\uD83D\uDE04\uD83D\uDE1A\uD83D\uDE13\uD83D\uDE13\uD83D\uDE01\uD83D\uDE01\uD83D\uDE1E\uD83D\uDE37\uD83D\uDE1E\uD83D\uDE1A\uD83D\uDE1A\uD83D\uDE04\uD83D\uDE1A\uD83D\uDE13\uD83D\uDE13")
//        list.add("\uD83D\uDE01\uD83D\uDE01\uD83D\uDE1E\uD83D\uDE37\uD83D\uDE1E\uD83D\uDE1A\uD83D\uDE1A\uD83D\uDE04\uD83D\uDE1A\uD83D\uDE13\uD83D\uDE13阿里里记录OMG您咯乖哦哦匿名咯lol您用肉也委屈啥小区\uD83D\uDE01\uD83D\uDE01\uD83D\uDE1E\uD83D\uDE37\uD83D\uDE1E\uD83D\uDE1A\uD83D\uDE1A\uD83D\uDE04\uD83D\uDE1A\uD83D\uDE13\uD83D\uDE13阿里里记录OMG您咯乖哦哦匿名咯lol您用肉也委屈啥小区")
        recyclerView.adapter =
            object : BaseQuickAdapter<String, BaseViewHolder>(R.layout.item_string, list) {

                override fun convert(helper: BaseViewHolder, item: String) {
                    val spb = SpannableStringBuilder(item)
                    spb.setSpan(QCSpan(), 0, item.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    helper.getView<QDQQFaceView>(R.id.tv_face).text = spb
                    val ot = helper.getView<QDQQFaceView>(R.id.tv_face)
                    ot.setSelectable(true)
                    ot.setOnTextSelectListener(object : SelectableHelper.OnTextSelectListener {

                        override fun onTextSelected(content: CharSequence?, start: Int, end: Int) {
                            Log.d(TAG,"文本----->${content}<----------> start->$start,end->$end")
                        }

                        override fun onPerformShow(point: Point) {
                            Log.d(TAG,"弹窗显示")
                        }

                        override fun onPerformDismiss() {
                            Log.d(TAG,"弹窗隐藏")
                        }

                    })
                    helper.getView<View>(R.id.button).setOnClickListener {
                        ot.turnOffSelectable()
                    }
                }
            }
        val texr = "21321231321"
        button.setOnClickListener {

        }
    }
}


class QCSpan() : QMUITouchableSpan(Color.BLACK, Color.BLACK, Color.WHITE, Color.WHITE) {
    override fun onSpanClick(widget: View?) {

    }

}
