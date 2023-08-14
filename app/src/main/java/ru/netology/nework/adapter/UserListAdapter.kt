package ru.netology.nework.adapter

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import ru.netology.nework.dto.User

class UserListAdapter(private val context: Context, private val userList: List<User>) : BaseAdapter() {

    override fun getCount(): Int = userList.size

    override fun getItem(position: Int): Any = userList[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val user = getItem(position) as User

        val textView = TextView(context)
        textView.text = user.name
        textView.setOnClickListener {
            // Handle click action here
            // You can show a fragment or dialog with user details
            // based on the clicked user's information
        }

        return textView
    }
}