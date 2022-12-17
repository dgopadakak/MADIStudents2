package com.example.madistudents

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment

class MyDialogFragmentDelExam: DialogFragment()
{
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog
    {
        val arguments: Bundle? = arguments
        val examName = arguments?.getString("exam")
        val builder = AlertDialog.Builder(activity)
        builder.setMessage("Будет удален экзамен по предмету: $examName")
            .setTitle("Внимание!")
            .setPositiveButton("Продолжить"
            ) { _, _ -> (activity as MainActivity?)?.delExam() }
            .setNegativeButton("Отмена") { _, _ -> }
        return builder.create()
    }
}