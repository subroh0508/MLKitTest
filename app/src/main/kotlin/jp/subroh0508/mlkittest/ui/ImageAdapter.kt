package jp.subroh0508.mlkittest.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import jp.subroh0508.mlkittest.R
import kotlinx.android.synthetic.main.listitem_image.view.*

class ImageAdapter(private val context: Context?, private val images: List<String>) : RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {
    var onSelectImage: (String) -> Unit = {}

    override fun getItemCount() = images.size

    override fun getItemId(position: Int) = images[position].hashCode().toLong()

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.itemView.setOnClickListener(null)

        val thumb = BitmapFactory.decodeStream(
                context?.assets?.open(images[position]),
                null,
                BitmapFactory.Options().also { it.inSampleSize = 8 }
        )

        holder.itemView.image.setImageBitmap(thumb)
        holder.itemView.setOnClickListener { onSelectImage(images[position]) }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder
        = ImageViewHolder(LayoutInflater.from(context ?: throw IllegalStateException()).inflate(R.layout.listitem_image, parent, false))

    class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view)
}