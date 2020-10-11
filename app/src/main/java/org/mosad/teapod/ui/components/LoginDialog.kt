/**
 * ProjectLaogai
 *
 * Copyright 2019-2020  <seil0@mosad.xyz>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 *
 */

package org.mosad.teapod.ui.components

import android.content.Context
import android.widget.EditText
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.bottomsheets.setPeekHeight
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import org.mosad.teapod.R

class LoginDialog(val context: Context) {

    private val dialog = MaterialDialog(context, BottomSheet())

    private val editTextLogin: EditText
    private val editTextPassword: EditText

    var login = ""
    var password = ""

    init {
        dialog.title(R.string.login)
            .message(R.string.login_desc)
            .customView(R.layout.dialog_login)
            .positiveButton(R.string.save)
            .negativeButton(R.string.cancel)
            .setPeekHeight(900)

        editTextLogin = dialog.getCustomView().findViewById(R.id.edit_text_login)
        editTextPassword = dialog.getCustomView().findViewById(R.id.edit_text_password)

        // fix not working accent color
        //dialog.getActionButton(WhichButton.POSITIVE).updateTextColor(Preferences.colorAccent)
        //dialog.getActionButton(WhichButton.NEGATIVE).updateTextColor(Preferences.colorAccent)
    }

    fun positiveButton(func: LoginDialog.() -> Unit): LoginDialog = apply {
        dialog.positiveButton {
            login = editTextLogin.text.toString()
            password = editTextPassword.text.toString()

            func()
        }
    }

    fun negativeButton(func: LoginDialog.() -> Unit): LoginDialog = apply {
        dialog.negativeButton {
            func()
        }
    }

    fun show() {
        dialog.show()
    }

    fun show(func: LoginDialog.() -> Unit): LoginDialog = apply {
        func()

        editTextLogin.setText(login)
        editTextPassword.setText(password)

        show()
    }

    @Suppress("unused")
    fun dismiss() {
        dialog.dismiss()
    }

}