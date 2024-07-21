package com.fitness.myhealthfitness

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

class Packages : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var productArrayList: ArrayList<ProductList>
    private lateinit var tempArrayList: ArrayList<ProductList>
    private lateinit var database: DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_packages, container, false)

        recyclerView = root.findViewById(R.id.productRecycelrView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.setHasFixedSize(true)

        productArrayList = arrayListOf()
        tempArrayList = arrayListOf()
        database = FirebaseDatabase.getInstance().reference.child("MyFitness").child("packages")
        getProductData()

        return root
    }

    private fun getProductData() {
        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded) return

                if (snapshot.exists()) {
                    for (packageSnapshot in snapshot.children) {
                        val credit = packageSnapshot.child("credit").getValue(String::class.java)
                        val description =
                            packageSnapshot.child("description").getValue(String::class.java)
                        val packageName =
                            packageSnapshot.child("package_name").getValue(String::class.java)
                        val price = packageSnapshot.child("price").getValue(String::class.java)
                        val imageURL = packageSnapshot.child("image").getValue(String::class.java)
                        val packageId = packageSnapshot.key

                        if (credit != null && description != null && packageName != null && price != null && imageURL != null) {
                            val product = ProductList(
                                packageName,
                                credit.toInt(),
                                price,
                                description,
                                imageURL,
                                packageId
                            )
                            productArrayList.add(product)
                        }
                    }

                    tempArrayList.addAll(productArrayList)

                    if (isAdded) {
                        val adapter = ProductListAdapter(productArrayList, requireContext())
                        recyclerView.adapter = adapter

                        adapter.setOnItemClickListener(object :
                            ProductListAdapter.onItemClickListener {
                            override fun onItemClick(position: Int) {
                                val intent = Intent(requireContext(), ProductPackages::class.java)
                                intent.putExtra(
                                    "productTitle",
                                    productArrayList[position].productTitle
                                )
                                intent.putExtra(
                                    "productCredits",
                                    productArrayList[position].productCredits.toString()
                                )
                                intent.putExtra(
                                    "productPrice",
                                    productArrayList[position].productPrice
                                )
                                intent.putExtra(
                                    "productDescription",
                                    productArrayList[position].productDescription
                                )
                                intent.putExtra(
                                    "productImage",
                                    productArrayList[position].productImageURL
                                )
                                startActivity(intent)
                            }
                        })
                    }
                } else {
                    if (isAdded) {
                        Toast.makeText(
                            requireContext(),
                            "No packages found in database",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                if (isAdded) {
                    Toast.makeText(
                        requireContext(),
                        "Failed to retrieve packages: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        })
    }
}
