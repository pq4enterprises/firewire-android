package com.pioneer.nycfirewire.adapter

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.getkeepsafe.taptargetview.TapTarget
import com.getkeepsafe.taptargetview.TapTargetSequence
import com.pioneer.nycfirewire.model.incident.response.Incident
import com.pioneer.nycfirewire.R
import com.pioneer.nycfirewire.activity.FeedFilterOrCommentsActivity
import com.pioneer.nycfirewire.activity.WireDetailActivity
import com.pioneer.nycfirewire.prefs
import com.pioneer.nycfirewire.utils.Constants
import com.pioneer.nycfirewire.utils.DateUtils
import com.pioneer.nycfirewire.utils.IntentUtils.BUN_WIRE_DETAILS
import com.pioneer.nycfirewire.utils.NAV_COMMENT_LIST
import com.pioneer.nycfirewire.utils.gone
import com.pioneer.nycfirewire.utils.visible

class PagingAdapter( private val listener: IncidentClickListener) : PagingDataAdapter<Incident, PagingAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Incident>() {
            override fun areItemsTheSame(old: Incident, new: Incident) = old._id == new._id
            override fun areContentsTheSame(old: Incident, new: Incident) = old == new
        }
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        private val title = view.findViewById<TextView>(R.id.tv_title)
        private val desc  = view.findViewById<TextView>(R.id.tv_desc)
        private val ivBanner  = view.findViewById<ImageView>(R.id.iv_banner)
        private val ivRating  = view.findViewById<ImageView>(R.id.iv_rating)
        private val tvDateTime  = view.findViewById<TextView>(R.id.tv_date_time)
        private val tvAddress  = view.findViewById<TextView>(R.id.tv_address)
        private val tvRateCount  = view.findViewById<TextView>(R.id.tv_rate_count)
        private val tvCommentCount  = view.findViewById<TextView>(R.id.tv_comment_count)
        private val ivCommand  = view.findViewById<ImageView>(R.id.iv_command)
        private val clContainer  = view.findViewById<ConstraintLayout>(R.id.cl_container)
        private val ivShare  = view.findViewById<ImageView>(R.id.iv_share)
        fun bind(it: Incident) {
            val context = itemView.context

            val pos = bindingAdapterPosition
            if(pos==0){
                listener.appIntroTour(title,ivRating, ivCommand,ivShare)
            }


           title.text = it.field1Value
            desc.text  = it.field2Value
            if(it.field2Value?.isNotEmpty() == true)desc.visible() else desc.gone()

            Glide.with(context)
                .load(it.featuredImageUrl)
                .into(ivBanner)

            if(it.isLiked)
                ivRating.setImageResource(R.drawable.ic_rating_red)
            else ivRating.setImageResource(R.drawable.ic_rating)

            ivRating.setOnClickListener { view->
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener.onRatingClicked(it,position)
                }
            }

            if(!it.createdAt.isNullOrEmpty()) {
                tvDateTime.text =
                    DateUtils.formatDateTime(it.createdAt.toString())
            }

            if(it.featuredImageUrl.isNullOrEmpty()){
                ivBanner.gone()
            }else{
                Glide.with(context)
                    .load(it.featuredImageUrl)
                    .into(ivBanner)
                ivBanner.visible()
            }
            var subLocalityName= if(it.subLocalityDetails?.isNotEmpty()==true) ", ".plus(it.subLocalityDetails.get(0).name) else ""
            tvAddress.text= it.field3Value.plus(subLocalityName)
            tvRateCount.text= context.getString(R.string.star,it.likeCount)
            val count= if(it.commentCount.isNullOrEmpty())"0" else it.commentCount

            if(count.toInt()>1)
                tvCommentCount.text= context.getString(R.string.comments,count)
            else tvCommentCount.text= context.getString(R.string.comment,count)

            ivCommand.setOnClickListener { view->
                val intent= Intent(context, FeedFilterOrCommentsActivity::class.java)
                intent.putExtra(NAV_COMMENT_LIST,it._id.toString())
                context.startActivity(intent)
            }

            clContainer.setOnClickListener { view->
                val bundle= Bundle()
                bundle.putParcelable(BUN_WIRE_DETAILS,it)
                val intent = Intent(context, WireDetailActivity::class.java)
                intent.putExtra(BUN_WIRE_DETAILS, bundle)
                context.startActivity(intent)
            }
            ivShare.setOnClickListener {view->
                val shareContent= it.field1Value.plus("\n").plus(it.address).plus("\n").plus(
                    Constants.PLAY_STORE_URL)
                 shareText(context,shareContent)
            }

        }


    }



    fun shareText(context: Context, text: String) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)  // Add the text to share
            type = "text/plain"  // Specify the MIME type as text
        }

        // Show a system chooser dialog to pick the app for sharing
        context.startActivity(Intent.createChooser(sendIntent, "Share via"))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_wire, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        getItem(position)?.let { holder.bind(it) }
    }
}

interface IncidentClickListener {
    fun onRatingClicked(incident: Incident,pos: Int)
    fun appIntroTour(tvTitle: TextView, ivRating: ImageView, ivCommand: ImageView, ivShare: ImageView)
}
