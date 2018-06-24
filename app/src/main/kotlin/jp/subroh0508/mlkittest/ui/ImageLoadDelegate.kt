package jp.subroh0508.mlkittest.ui

import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.widget.LinearLayout

class ImageLoadDelegate {
    private lateinit var adapter: ImageAdapter

    fun loadImagesFromAsset(recyclerView: RecyclerView, filenames: List<String>, onSelect: (String) -> Unit) {
        val context = recyclerView.context

        adapter = ImageAdapter(context, filenames).also {
            it.onSelectImage = onSelect
        }

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(context, LinearLayout.HORIZONTAL, false)
    }
}