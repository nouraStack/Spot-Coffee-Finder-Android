package com.example.spot.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.spot.R;
import com.example.spot.adapters.ChatMessageAdapter;
import com.example.spot.databinding.FragmentChatBinding;
import com.example.spot.models.ChatMessage;
import com.example.spot.utils.FirebaseHelper;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

public class ChatFragment extends Fragment {

    private FragmentChatBinding binding;
    private ChatMessageAdapter adapter;
    private final List<ChatMessage> messages = new ArrayList<>();
    private String bookingId;
    private String cafeId;
    private DatabaseReference chatRef;
    private ChildEventListener chatListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentChatBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            bookingId = getArguments().getString("bookingId", "");
            cafeId = getArguments().getString("cafeId", "");
        }

        if (bookingId == null || bookingId.isEmpty()) {
            Toast.makeText(requireContext(), "Chat not available", Toast.LENGTH_SHORT).show();
            if (getActivity() != null) getActivity().onBackPressed();
            return;
        }

        binding.tvChatTitle.setText(cafeId != null && !cafeId.isEmpty() ? "Chat" : "Chat with Provider");

        String currentUserId = FirebaseHelper.getInstance().getCurrentUserId();
        adapter = new ChatMessageAdapter(messages, currentUserId);
        binding.rvMessages.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvMessages.setAdapter(adapter);

        binding.btnBack.setOnClickListener(v -> {
            if (getActivity() != null) getActivity().onBackPressed();
        });

        binding.btnSend.setOnClickListener(v -> sendMessage());

        loadMessages();
    }

    private void loadMessages() {
        chatRef = FirebaseDatabase.getInstance().getReference("Chats").child(bookingId);
        chatListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                ChatMessage msg = snapshot.getValue(ChatMessage.class);
                if (msg != null) {
                    msg.setMessageId(snapshot.getKey());
                    messages.add(msg);
                    adapter.notifyItemInserted(messages.size() - 1);
                    if (binding != null) {
                        binding.rvMessages.scrollToPosition(messages.size() - 1);
                    }
                }
            }

            @Override public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
            @Override public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };
        chatRef.addChildEventListener(chatListener);
    }

    private void sendMessage() {
        String text = binding.etMessage.getText() != null ? binding.etMessage.getText().toString().trim() : "";
        if (text.isEmpty()) return;

        String senderId = FirebaseHelper.getInstance().getCurrentUserId();
        if (senderId == null) {
            Toast.makeText(requireContext(), "Please login to send messages", Toast.LENGTH_SHORT).show();
            return;
        }

        String senderName = "";
        if (FirebaseHelper.getInstance().getAuth().getCurrentUser() != null
                && FirebaseHelper.getInstance().getAuth().getCurrentUser().getDisplayName() != null) {
            senderName = FirebaseHelper.getInstance().getAuth().getCurrentUser().getDisplayName();
        }

        String messageId = chatRef.push().getKey();
        if (messageId == null) return;

        ChatMessage msg = new ChatMessage(messageId, senderId, senderName, text, System.currentTimeMillis());
        chatRef.child(messageId).setValue(msg)
                .addOnSuccessListener(aVoid -> {
                    if (binding != null) binding.etMessage.setText("");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Failed to send message", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (chatRef != null && chatListener != null) {
            chatRef.removeEventListener(chatListener);
        }
        binding = null;
    }
}

