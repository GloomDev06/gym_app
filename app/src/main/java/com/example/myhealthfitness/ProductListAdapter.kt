package com.fitness.myhealthfitness

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*

class ProductListAdapter(
    private val productList: ArrayList<ProductList>,
    private val context: Context
) : RecyclerView.Adapter<ProductListAdapter.MyViewHolder>() {

    private lateinit var mListener: onItemClickListener
    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference
    private val userId: String? = getUserIdFromSharedPreferences(context)

    interface onItemClickListener {
        fun onItemClick(position: Int)
    }

    fun setOnItemClickListener(listener: onItemClickListener) {
        mListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.packages_list, parent, false)
        return MyViewHolder(itemView, mListener)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val currentItem = productList[position]
        holder.productTitle.text = "Package Name: ${currentItem.productTitle}"
        holder.productPrice.text = "Price: ${currentItem.productPrice}"
        holder.productCredits.text = "Credits: ${currentItem.productCredits}"

        Glide.with(holder.itemView)
            .load(currentItem.productImageURL)
            .into(holder.productImage)

        if (userId != null) {
            database.child("MyFitness").child("usersWishList").child(userId)
                .child(currentItem.packageId!!).get().addOnSuccessListener { dataSnapshot ->
                    if (dataSnapshot.exists() && !dataSnapshot.hasChild("deletedAt")) {
                        holder.heartImage.setImageResource(R.drawable.heart_fill)
                        holder.heartImage.tag = R.drawable.heart_fill
                    } else {
                        holder.heartImage.setImageResource(R.drawable.heart)
                        holder.heartImage.tag = R.drawable.heart
                    }
                }.addOnFailureListener {
                    holder.heartImage.setImageResource(R.drawable.heart)
                    holder.heartImage.tag = R.drawable.heart
                }
        }

        holder.heartImage.setOnClickListener {
            if (holder.heartImage.tag == R.drawable.heart_fill) {
                holder.heartImage.setImageResource(R.drawable.heart)
                holder.heartImage.tag = R.drawable.heart

                if (userId != null) {
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    val deletionTime = dateFormat.format(Date())

                    database.child("MyFitness").child("usersWishList").child(userId)
                        .child(currentItem.packageId!!)
                        .child("deletedAt").setValue(deletionTime)
                        .addOnSuccessListener {
                            Toast.makeText(
                                holder.itemView.context,
                                "Marked as deleted",
                                Toast.LENGTH_SHORT
                            ).show()
                        }.addOnFailureListener {
                            Toast.makeText(
                                holder.itemView.context,
                                "Failed to mark as deleted",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
            } else {
                holder.heartImage.setImageResource(R.drawable.heart_fill)
                holder.heartImage.tag = R.drawable.heart_fill

                if (userId != null) {
                    database.child("MyFitness").child("usersWishList").child(userId)
                        .child(currentItem.packageId!!)
                        .setValue(currentItem)
                        .addOnSuccessListener {
                            Toast.makeText(
                                holder.itemView.context,
                                "Added to wishlist",
                                Toast.LENGTH_SHORT
                            ).show()
                        }.addOnFailureListener {
                            Toast.makeText(
                                holder.itemView.context,
                                "Failed to add to wishlist",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
            }

            println("Position: $position, Package Name: ${currentItem.productTitle}")
            val packageId = currentItem.packageId ?: "Unknown"
        }

        holder.heartImage.tag = R.drawable.heart
    }

    override fun getItemCount(): Int {
        return productList.size
    }

    class MyViewHolder(itemView: View, listener: onItemClickListener) :
        RecyclerView.ViewHolder(itemView) {
        val productTitle: TextView = itemView.findViewById(R.id.productName)
        val productPrice: TextView = itemView.findViewById(R.id.productPrice)
        val productCredits: TextView = itemView.findViewById(R.id.productCredits)
        val productImage: ImageView = itemView.findViewById(R.id.productImageList)
        val heartImage: ImageView = itemView.findViewById(R.id.like)

        init {
            itemView.setOnClickListener {
                listener.onItemClick(adapterPosition)
            }
        }
    }

    private fun getUserIdFromSharedPreferences(context: Context): String? {
        val sharedPreferences = context.getSharedPreferences("loginPrefs", Context.MODE_PRIVATE)
        return sharedPreferences.getString("userId", null)
    }
}
