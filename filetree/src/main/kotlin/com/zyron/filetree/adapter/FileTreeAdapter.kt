package com.zyron.filetree.adapter

import android.content.Context
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.zyron.filetree.FileTree
import com.zyron.filetree.FileTreeAdapterUpdateListener
import com.zyron.filetree.Node
import com.zyron.filetree.R
import com.zyron.filetree.callback.FileTreeNodeDiffCallback
import com.zyron.filetree.interfaces.FileTreeEventListener
import com.zyron.filetree.provider.DefaultFileIconProvider
import com.zyron.filetree.provider.FileTreeIconProvider
import com.zyron.filetree.viewholder.FileTreeViewHolder
import java.nio.file.Files

class FileTreeAdapter(
    private val context: Context,
    private val fileTree: FileTree,
    private val fileTreeIconProvider: FileTreeIconProvider,
    private val fileTreeEventListener: FileTreeEventListener? = null
) : RecyclerView.Adapter<FileTreeViewHolder>(), FileTreeAdapterUpdateListener {

    @JvmOverloads
    constructor(
        context: Context, fileTree: FileTree, fileTreeEventListener: FileTreeEventListener? = null
    ) : this(context, fileTree, DefaultFileIconProvider(), fileTreeEventListener)

    private var selectedItemPosition: Int = RecyclerView.NO_POSITION
    private var nodes: MutableList<Node> = fileTree.getNodes().toMutableList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileTreeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.filetree_view_item, parent, false)
        return FileTreeViewHolder(view)
    }

    override fun onBindViewHolder(holder: FileTreeViewHolder, position: Int) {
        val node = nodes[position]

        val indentationDp = 12 * node.level
        val indentationPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, indentationDp.toFloat(), context.resources.displayMetrics
        ).toInt()

        val isRtl = context.resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL

        val layoutParams = holder.itemView.layoutParams as ViewGroup.MarginLayoutParams
        if (isRtl) {
            layoutParams.rightMargin = indentationPx
            layoutParams.leftMargin = 0

        } else {
            layoutParams.leftMargin = indentationPx
            layoutParams.rightMargin = 0

        }
        holder.itemView.layoutParams = layoutParams

        val chevronIcon = if (node.isExpanded) {
            fileTreeIconProvider.getChevronCollapseIcon()
        } else {
            fileTreeIconProvider.getChevronExpandIcon()
        }

        holder.itemView.setPadding(4, 8, 4, 8)
        holder.chevronIconView.setPadding(4, 0, 2, 0)
        holder.fileIconView.setPadding(2, 0, 4, 0)
        holder.fileNameView.setPadding(6, 7, 7, 6)

        holder.itemView.setBackgroundResource(R.drawable.item_background)

        holder.itemView.isSelected =
            position == selectedItemPosition && node.isExpanded && Files.isDirectory(node.file.toPath())

        if (Files.isDirectory(node.file.toPath())) {
            holder.chevronIconView.setImageDrawable(ContextCompat.getDrawable(context, chevronIcon))
            holder.chevronIconView.visibility = View.VISIBLE
            holder.fileIconView.setImageDrawable(
                ContextCompat.getDrawable(
                    context, fileTreeIconProvider.getFolderIcon()
                )
            )
            holder.fileNameView.text = node.file.name

            holder.itemView.setOnClickListener {
                val previousPosition = selectedItemPosition
                if (node.isExpanded) {
                    fileTree.collapseNode(node)
                } else {
                    fileTree.expandNode(node)
                }
                selectedItemPosition = holder.bindingAdapterPosition
                notifyItemChanged(selectedItemPosition)
                notifyItemChanged(previousPosition)
            }

            holder.itemView.setOnLongClickListener {
                fileTreeEventListener?.onFolderLongClick(node.file) ?: false
            }

        } else if (node.file.isFile) {
            holder.chevronIconView.setImageDrawable(ContextCompat.getDrawable(context, chevronIcon))
            holder.chevronIconView.visibility = View.INVISIBLE
            holder.fileIconView.setImageDrawable(
                ContextCompat.getDrawable(
                    context, fileTreeIconProvider.getIconForFile(node.file)
                )
            )
            holder.fileNameView.text = node.file.name

            holder.itemView.setOnClickListener {
                fileTreeEventListener?.onFileClick(node.file)
            }

            holder.itemView.setOnLongClickListener {
                fileTreeEventListener?.onFileLongClick(node.file) ?: false
            }
        }
    }

    override fun onFileTreeUpdated(startPosition: Int, itemCount: Int) {
        if (itemCount > 0) {
            notifyItemRangeInserted(startPosition, itemCount)
        } else {
            notifyItemRangeRemoved(startPosition, -itemCount)
        }
    }

    fun updateNodes(newNodes: List<Node>) {
        val diffResult = DiffUtil.calculateDiff(FileTreeNodeDiffCallback(nodes, newNodes))
        nodes.clear()
        nodes.addAll(newNodes)
        diffResult.dispatchUpdatesTo(this)
    }

    override fun getItemCount(): Int {
        return nodes.size
    }
}