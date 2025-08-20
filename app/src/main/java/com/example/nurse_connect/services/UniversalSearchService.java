package com.example.nurse_connect.services;

import android.util.Log;

import com.example.nurse_connect.models.StudyMaterial;
import com.example.nurse_connect.models.User;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UniversalSearchService {
    private static final String TAG = "UniversalSearchService";
    private FirebaseFirestore firestore;

    public interface SearchCallback {
        void onSuccess(SearchResults results);
        void onFailure(Exception e);
    }

    public static class SearchResults {
        private List<StudyMaterial> studyMaterials = new ArrayList<>();
        private List<User> users = new ArrayList<>();
        private List<Map<String, Object>> groups = new ArrayList<>();
        private List<Map<String, Object>> posts = new ArrayList<>();
        private List<Map<String, Object>> tasks = new ArrayList<>();

        // Getters and setters
        public List<StudyMaterial> getStudyMaterials() { return studyMaterials; }
        public void setStudyMaterials(List<StudyMaterial> studyMaterials) { this.studyMaterials = studyMaterials; }

        public List<User> getUsers() { return users; }
        public void setUsers(List<User> users) { this.users = users; }

        public List<Map<String, Object>> getGroups() { return groups; }
        public void setGroups(List<Map<String, Object>> groups) { this.groups = groups; }

        public List<Map<String, Object>> getPosts() { return posts; }
        public void setPosts(List<Map<String, Object>> posts) { this.posts = posts; }

        public List<Map<String, Object>> getTasks() { return tasks; }
        public void setTasks(List<Map<String, Object>> tasks) { this.tasks = tasks; }

        public int getTotalResults() {
            return studyMaterials.size() + users.size() + groups.size() + posts.size() + tasks.size();
        }
    }

    public UniversalSearchService() {
        firestore = FirebaseFirestore.getInstance();
    }

    public void searchAll(String query, SearchCallback callback) {
        if (query == null || query.trim().isEmpty()) {
            callback.onSuccess(new SearchResults());
            return;
        }

        String searchQuery = query.trim().toLowerCase();
        SearchResults results = new SearchResults();
        
        // Counter to track completed searches
        final int[] completedSearches = {0};
        final int totalSearches = 5; // study materials, users, groups, posts, tasks

        // Search study materials
        searchStudyMaterials(searchQuery, new SearchCallback() {
            @Override
            public void onSuccess(SearchResults materialResults) {
                results.setStudyMaterials(materialResults.getStudyMaterials());
                checkAndReturnResults(++completedSearches[0], totalSearches, results, callback);
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Failed to search study materials", e);
                checkAndReturnResults(++completedSearches[0], totalSearches, results, callback);
            }
        });

        // Search users
        searchUsers(searchQuery, new SearchCallback() {
            @Override
            public void onSuccess(SearchResults userResults) {
                results.setUsers(userResults.getUsers());
                checkAndReturnResults(++completedSearches[0], totalSearches, results, callback);
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Failed to search users", e);
                checkAndReturnResults(++completedSearches[0], totalSearches, results, callback);
            }
        });

        // Search groups
        searchGroups(searchQuery, new SearchCallback() {
            @Override
            public void onSuccess(SearchResults groupResults) {
                results.setGroups(groupResults.getGroups());
                checkAndReturnResults(++completedSearches[0], totalSearches, results, callback);
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Failed to search groups", e);
                checkAndReturnResults(++completedSearches[0], totalSearches, results, callback);
            }
        });

        // Search posts
        searchPosts(searchQuery, new SearchCallback() {
            @Override
            public void onSuccess(SearchResults postResults) {
                results.setPosts(postResults.getPosts());
                checkAndReturnResults(++completedSearches[0], totalSearches, results, callback);
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Failed to search posts", e);
                checkAndReturnResults(++completedSearches[0], totalSearches, results, callback);
            }
        });

        // Search tasks
        searchTasks(searchQuery, new SearchCallback() {
            @Override
            public void onSuccess(SearchResults taskResults) {
                results.setTasks(taskResults.getTasks());
                checkAndReturnResults(++completedSearches[0], totalSearches, results, callback);
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Failed to search tasks", e);
                checkAndReturnResults(++completedSearches[0], totalSearches, results, callback);
            }
        });
    }

    private void checkAndReturnResults(int completed, int total, SearchResults results, SearchCallback callback) {
        if (completed >= total) {
            callback.onSuccess(results);
        }
    }

    private void searchStudyMaterials(String query, SearchCallback callback) {
        firestore.collection("study_materials")
                .whereGreaterThanOrEqualTo("title", query)
                .whereLessThanOrEqualTo("title", query + '\uf8ff')
                .limit(10)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    SearchResults results = new SearchResults();
                    List<StudyMaterial> materials = new ArrayList<>();
                    
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        StudyMaterial material = doc.toObject(StudyMaterial.class);
                        if (material != null) {
                            material.setId(doc.getId());
                            materials.add(material);
                        }
                    }
                    
                    // Also search by category and description
                    searchStudyMaterialsByCategory(query, materials, results, callback);
                })
                .addOnFailureListener(callback::onFailure);
    }

    private void searchStudyMaterialsByCategory(String query, List<StudyMaterial> existingMaterials, 
                                               SearchResults results, SearchCallback callback) {
        firestore.collection("study_materials")
                .whereGreaterThanOrEqualTo("category", query)
                .whereLessThanOrEqualTo("category", query + '\uf8ff')
                .limit(10)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        StudyMaterial material = doc.toObject(StudyMaterial.class);
                        if (material != null) {
                            material.setId(doc.getId());
                            // Avoid duplicates
                            boolean exists = existingMaterials.stream()
                                    .anyMatch(m -> m.getId().equals(material.getId()));
                            if (!exists) {
                                existingMaterials.add(material);
                            }
                        }
                    }
                    
                    results.setStudyMaterials(existingMaterials);
                    callback.onSuccess(results);
                })
                .addOnFailureListener(callback::onFailure);
    }

    private void searchUsers(String query, SearchCallback callback) {
        firestore.collection("users")
                .whereGreaterThanOrEqualTo("displayName", query)
                .whereLessThanOrEqualTo("displayName", query + '\uf8ff')
                .limit(10)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    SearchResults results = new SearchResults();
                    List<User> users = querySnapshot.toObjects(User.class);
                    results.setUsers(users);
                    callback.onSuccess(results);
                })
                .addOnFailureListener(callback::onFailure);
    }

    private void searchGroups(String query, SearchCallback callback) {
        firestore.collection("group_chats")
                .whereEqualTo("isPublic", true)
                .whereGreaterThanOrEqualTo("groupName", query)
                .whereLessThanOrEqualTo("groupName", query + '\uf8ff')
                .limit(10)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    SearchResults results = new SearchResults();
                    List<Map<String, Object>> groups = new ArrayList<>();
                    
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Map<String, Object> group = doc.getData();
                        group.put("id", doc.getId());
                        groups.add(group);
                    }
                    
                    results.setGroups(groups);
                    callback.onSuccess(results);
                })
                .addOnFailureListener(callback::onFailure);
    }

    private void searchPosts(String query, SearchCallback callback) {
        firestore.collection("posts")
                .whereGreaterThanOrEqualTo("content", query)
                .whereLessThanOrEqualTo("content", query + '\uf8ff')
                .limit(10)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    SearchResults results = new SearchResults();
                    List<Map<String, Object>> posts = new ArrayList<>();
                    
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Map<String, Object> post = doc.getData();
                        post.put("id", doc.getId());
                        posts.add(post);
                    }
                    
                    results.setPosts(posts);
                    callback.onSuccess(results);
                })
                .addOnFailureListener(callback::onFailure);
    }

    private void searchTasks(String query, SearchCallback callback) {
        firestore.collection("study_tasks")
                .whereGreaterThanOrEqualTo("title", query)
                .whereLessThanOrEqualTo("title", query + '\uf8ff')
                .limit(10)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    SearchResults results = new SearchResults();
                    List<Map<String, Object>> tasks = new ArrayList<>();
                    
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Map<String, Object> task = doc.getData();
                        task.put("id", doc.getId());
                        tasks.add(task);
                    }
                    
                    results.setTasks(tasks);
                    callback.onSuccess(results);
                })
                .addOnFailureListener(callback::onFailure);
    }
}
