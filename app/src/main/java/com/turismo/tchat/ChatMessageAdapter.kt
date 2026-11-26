package com.turismo.tchat

import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

class ChatMessageAdapter(
    private val messages: MutableList<ChatMessage>,
    private val onReplyClick: (ChatMessage) -> Unit
) : RecyclerView.Adapter<ChatMessageAdapter.MessageViewHolder>() {

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val root: LinearLayout = itemView.findViewById(R.id.messageRowRoot)
        val bubble: CardView = itemView.findViewById(R.id.cardMessageBubble)
        val tvSender: TextView = itemView.findViewById(R.id.tvMsgSender)
        val tvReplyQuote: TextView = itemView.findViewById(R.id.tvMsgReplyQuote)
        val tvBody: TextView = itemView.findViewById(R.id.tvMsgBody)
        val tvTime: TextView = itemView.findViewById(R.id.tvMsgTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val msg = messages[position]

        holder.root.gravity = if (msg.isOwn) Gravity.END else Gravity.START
        holder.bubble.setCardBackgroundColor(
            if (msg.isOwn) Color.parseColor("#8A2BE2") else Color.parseColor("#26283E")
        )

        if (msg.isOwn) {
            holder.tvSender.visibility = View.GONE
        } else {
            holder.tvSender.visibility = View.VISIBLE
            holder.tvSender.text = msg.sender
        }

        if (msg.replyPreview.isNullOrBlank()) {
            holder.tvReplyQuote.visibility = View.GONE
        } else {
            holder.tvReplyQuote.visibility = View.VISIBLE
            holder.tvReplyQuote.text = msg.replyPreview
        }

        holder.tvBody.text = msg.text
        holder.tvTime.text = msg.timestamp

        holder.bubble.setOnClickListener {
            onReplyClick(msg)
        }
    }

    override fun getItemCount(): Int = messages.size
}
