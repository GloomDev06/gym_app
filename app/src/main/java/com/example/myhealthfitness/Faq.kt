package com.fitness.myhealthfitness

import android.os.Bundle
import android.text.Html
import android.text.Spanned
import android.text.SpannedString
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.database.*

class Faq : Fragment() {

    private lateinit var databaseReference: DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_faq, container, false)

//        databaseReference = FirebaseDatabase.getInstance().reference.child("MyFitness").child("faq")
//
//        databaseReference.addValueEventListener(object : ValueEventListener {
//            override fun onDataChange(snapshot: DataSnapshot) {
//                val introHtml = snapshot.child("intro").getValue(String::class.java) ?: ""
//                val footerHtml = snapshot.child("footer").getValue(String::class.java) ?: ""
//
//                val introSpanned = fromHtml(introHtml)
//                val footerSpanned = fromHtml(footerHtml)
//
//                root.findViewById<TextView>(R.id.faqIntro).text = introSpanned
//                root.findViewById<TextView>(R.id.faqOutro).text = footerSpanned
//
//                val faqLayout = root.findViewById<LinearLayout>(R.id.faqLayout)
//                faqLayout.removeAllViews()
//
//                for (categorySnapshot in snapshot.children) {
//                    Toast.makeText(requireContext(), "$categorySnapshot", Toast.LENGTH_SHORT)
//                        .show()
//                    if (categorySnapshot.key != "intro" && categorySnapshot.key != "footer" && categorySnapshot.key != "null") {
//                        val categoryName = categorySnapshot.key ?: ""
//                        val faqList = categorySnapshot.children.toList()
//
//                        val headerView = inflater.inflate(R.layout.faquestions, null)
//                        val titleTextView = headerView.findViewById<TextView>(R.id.title)
//                        titleTextView.text = categoryName
//
//                        faqLayout.addView(headerView)
//
//                        var count = 1
//                        for (faqItemSnapshot in faqList) {
//
//                            if (faqItemSnapshot.value != null && faqItemSnapshot.value is Map<*, *>) {
//                                val question =
//                                    faqItemSnapshot.child("question").getValue(String::class.java)
//                                        ?: ""
//                                val answerHtml =
//                                    faqItemSnapshot.child("answer").getValue(String::class.java)
//                                        ?: ""
//
//                                val answerSpanned = fromHtml(answerHtml)
//
//                                val faqItemView = inflater.inflate(R.layout.faquestions, null)
//                                val countTextView = faqItemView.findViewById<TextView>(R.id.count)
//                                val questionTextView =
//                                    faqItemView.findViewById<TextView>(R.id.question)
//                                val answerTextView = faqItemView.findViewById<TextView>(R.id.answer)
//
//                                countTextView.text = count.toString()
//                                questionTextView.text = question
//                                answerTextView.text = answerSpanned
//
//                                faqLayout.addView(faqItemView)
//
//                                count++
//                            }
//                        }
//                    }
//                }
//            }
//
//            private fun fromHtml(html: String): Spanned {
//                return if (html.isNotBlank()) {
//                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
//                        Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
//                    } else {
//                        @Suppress("DEPRECATION")
//                        Html.fromHtml(html)
//                    }
//                } else {
//                    SpannedString("")
//                }
//            }
//
//            override fun onCancelled(error: DatabaseError) {
//                Toast.makeText(
//                    requireContext(),
//                    "Failed to read FAQ: ${error.message}",
//                    Toast.LENGTH_SHORT
//                ).show()
//            }
//        })

        return root
    }
}
