package com.fitness.myhealthfitness

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.fragment.app.Fragment
import com.example.myhealthfitness.ZoomMeeting
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class FragmentHome : Fragment() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var database: DatabaseReference
    private var userId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_home, container, false)

        // Initialize SharedPreferences
        sharedPreferences = activity?.getSharedPreferences("loginPrefs", Context.MODE_PRIVATE)!!
        userId = sharedPreferences?.getString("userId", "-")

        // Initialize Firebase database reference
        database = FirebaseDatabase.getInstance().reference

        // Fetch user data from Firebase
        fetchUserData(root)

        // Fetch Zoom meeting data from Firebase
        fetchZoomMeetingData(root)

        return root
    }

    private fun fetchUserData(root: View) {
        userId?.let { id ->
            database.child("MyFitness").child("userDetails").child(id)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (!isAdded) return

                        if (snapshot.exists()) {
                            val user = snapshot.getValue(User::class.java)
                            user?.let {
                                root.findViewById<TextView>(R.id.textUserName).text = "Welcome, ${it.username}!!"
                                root.findViewById<TextView>(R.id.userId).text = "Your User Id: ${it.userId}"
                                root.findViewById<TextView>(R.id.currentCredits).text = "Your Current Credits : ${it.available_credits}"
                                root.findViewById<TextView>(R.id.email).text = "Logged in as ${it.email}"
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        if (isAdded) {
                            Toast.makeText(requireContext(), "Failed to fetch user data: ${error.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                })
        }
    }

    private fun fetchZoomMeetingData(root: View) {
        database.child("MyFitness").child("zoomMeeting").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded) return

                if (snapshot.exists()) {
                    val zoomMeetings = mutableListOf<ZoomMeeting>()
                    for (dataSnapshot in snapshot.children) {
                        val zoomMeeting = dataSnapshot.getValue(ZoomMeeting::class.java)
                        zoomMeeting?.let { zoomMeetings.add(it) }
                    }
                    displayZoomMeetings(root, zoomMeetings)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                if (isAdded) {
                    Toast.makeText(requireContext(), "Failed to fetch Zoom meetings: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun displayZoomMeetings(root: View, zoomMeetings: List<ZoomMeeting>) {
        if (!isAdded) return

        val zoomMeetingContainer = root.findViewById<ViewGroup>(R.id.shakeRequestsContainer)
        zoomMeetings.forEach { zoomMeeting ->
            val meetingView = layoutInflater.inflate(R.layout.zoom_meet, zoomMeetingContainer, false)

            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val meetingTime = dateFormat.format(Date(zoomMeeting.timing))

            meetingView.findViewById<TextView>(R.id.zoomTime).text = "Time: $meetingTime"
            meetingView.findViewById<TextView>(R.id.description).text = Html.fromHtml(zoomMeeting.text).toString()
            meetingView.findViewById<AppCompatButton>(R.id.btnZoomMeeting).setOnClickListener {
                joinZoomMeeting(zoomMeeting)
            }
            zoomMeetingContainer.addView(meetingView)
        }
    }

    private fun joinZoomMeeting(zoomMeeting: ZoomMeeting) {
        val meetingUrl = "zoomus://zoom.us/join?confno=${zoomMeeting.link}&pwd=${zoomMeeting.password}"
        val zoomAppIntent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(meetingUrl)
        }
        try {
            startActivity(zoomAppIntent)
        } catch (e: ActivityNotFoundException) {
            val zoomPlayStoreIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://play.google.com/store/apps/details?id=us.zoom.videomeetings")
            }
            startActivity(zoomPlayStoreIntent)
        }
    }
}
