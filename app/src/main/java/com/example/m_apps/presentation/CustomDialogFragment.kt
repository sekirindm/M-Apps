package com.example.m_apps.presentation

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.example.m_apps.R

object CustomDialogFragment {

    fun customDialogWindow(context: Context, onNameEntered: (String) -> Unit) {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.custom_dialog_enter)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val editText = dialog.findViewById<EditText>(R.id.tv_name)

        val buttonOk = dialog.findViewById<Button>(R.id.b_ok)
        buttonOk.setOnClickListener {
            val name = editText.text.toString()
            onNameEntered(name)
            dialog.dismiss()
        }

        dialog.show()
    }

    fun customDialog(context: Context, name: String, lng: String, onDrawLine: () -> Unit, delete:() -> Unit) {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.custom_dialog_place)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val tvLng: TextView = dialog.findViewById(R.id.tv_lng)
        val tvName: TextView = dialog.findViewById(R.id.tv_name)
        val drawLineButton: Button = dialog.findViewById(R.id.b_route)
        val deleteButton: Button = dialog.findViewById(R.id.b_delete)

        tvLng.text = lng
        tvName.text = name

        deleteButton.setOnClickListener {
            delete()
            dialog.dismiss()
        }

        drawLineButton.setOnClickListener {
            onDrawLine()
            dialog.dismiss()
        }

        dialog.show()
    }
}