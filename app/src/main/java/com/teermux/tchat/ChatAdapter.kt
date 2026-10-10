package com.teermux.tchat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class ChatAdapter(
    private val messages: List<ChatMessage>,
    private val onItemLongClick: (ChatMessage) -> Unit = {}
) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvAvatarIcon: TextView = itemView.findViewById(R.id.tvAvatarIcon)
        val tvSenderName: TextView = itemView.findViewById(R.id.tvSenderName)
        val tvTimestamp: TextView = itemView.findViewById(R.id.tvTimestamp)
        val tvMessageBody: TextView = itemView.findViewById(R.id.tvMessageBody)
        val layoutReplyPreview: View = itemView.findViewById(R.id.layoutReplyPreview)
        val tvReplySender: TextView = itemView.findViewById(R.id.tvReplySender)
        val tvReplyText: TextView = itemView.findViewById(R.id.tvReplyText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_message, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val msg = messages[position]

        holder.tvSenderName.text = if (msg.senderName.isNotEmpty()) msg.senderName else "Anonymous"
        holder.tvMessageBody.text = if (msg.text.isNotEmpty()) msg.text else "[Empty Message]"
        holder.tvTimestamp.text = msg.timestamp

        holder.tvAvatarIcon.text = if (msg.senderName.isNotEmpty()) {
            msg.senderName.take(1).uppercase(Locale.getDefault())
        } else {
            "?"
        }

        if (!msg.replyToText.isNullOrEmpty()) {
            holder.layoutReplyPreview.visibility = View.VISIBLE
            holder.tvReplySender.text = msg.replyToSender ?: ""
            holder.tvReplyText.text = msg.replyToText
        } else {
            holder.layoutReplyPreview.visibility = View.GONE
        }

        holder.itemView.setOnLongClickListener {
            onItemLongClick(msg)
            true
        }
    }

    override fun getItemCount(): Int = messages.size
}
