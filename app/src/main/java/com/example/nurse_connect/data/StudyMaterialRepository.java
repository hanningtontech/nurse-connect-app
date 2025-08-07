package com.example.nurse_connect.data;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;

import com.example.nurse_connect.models.StudyMaterial;
import com.example.nurse_connect.utils.PdfThumbnailGenerator;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class StudyMaterialRepository {
    private static final String TAG = "StudyMaterialRepository";
    private FirebaseFirestore firestore;
    private FirebaseStorage storage;
    private FirebaseAuth auth;
    
    public StudyMaterialRepository() {
        firestore = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        auth = FirebaseAuth.getInstance();
    }
    
    public interface StudyMaterialCallback {
        void onSuccess(List<StudyMaterial> materials);
        void onFailure(Exception e);
    }
    
    public interface UploadCallback {
        void onSuccess(StudyMaterial material);
        void onProgress(double progress);
        void onFailure(Exception e);
    }
    
    public interface ThumbnailCallback {
        void onSuccess(String thumbnailUrl);
    }
    
    public interface DownloadCallback {
        void onSuccess(Uri downloadUri);
        void onFailure(Exception e);
    }
    
    // Upload study material to Firebase Storage and Firestore
    public void uploadStudyMaterial(Context context, Uri fileUri, String title, String description, 
                                   String category, String authorId, 
                                   String authorName, String privacy, UploadCallback callback) {
        
        String fileName = "study_materials/" + UUID.randomUUID().toString() + ".pdf";
        StorageReference storageRef = storage.getReference().child(fileName);
        
        UploadTask uploadTask = storageRef.putFile(fileUri);
        
        uploadTask.addOnProgressListener(taskSnapshot -> {
            double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
            callback.onProgress(progress);
        }).addOnSuccessListener(taskSnapshot -> {
            // Get download URL
            storageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                Log.d(TAG, "PDF uploaded successfully, starting thumbnail generation");
                // Generate thumbnail
                generateAndUploadThumbnail(context, fileUri, materialId -> {
                    // Create study material object
                    StudyMaterial material = new StudyMaterial();
                    material.setId(UUID.randomUUID().toString());
                    material.setTitle(title);
                    material.setDescription(description);
                    material.setCategory(category);
                    material.setAuthorId(authorId);
                    material.setAuthorName(authorName);
                    material.setPrivacy(privacy);
                    material.setFileUrl(downloadUri.toString());
                    material.setFileName(fileName);
                    material.setFileSize(taskSnapshot.getTotalByteCount());
                    material.setUploadDate(System.currentTimeMillis());
                    material.setDownloads(0);
                    material.setLikes(0);
                    
                    // Set thumbnail URL if generated
                    if (materialId != null) {
                        material.setThumbnailURL(materialId);
                        Log.d(TAG, "Thumbnail URL set: " + materialId);
                    } else {
                        Log.w(TAG, "No thumbnail URL generated, material will have no thumbnail");
                    }
                    
                    // Save to Firestore
                    firestore.collection("study_materials")
                            .document(material.getId())
                            .set(material)
                            .addOnSuccessListener(aVoid -> {
                                callback.onSuccess(material);
                            })
                            .addOnFailureListener(callback::onFailure);
                });
            }).addOnFailureListener(callback::onFailure);
        }).addOnFailureListener(callback::onFailure);
    }
    
    // Get all study materials
    public void getAllStudyMaterials(StudyMaterialCallback callback) {
        firestore.collection("study_materials")
                .orderBy("uploadDate", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<StudyMaterial> materials = new ArrayList<>();
                    for (DocumentSnapshot document : querySnapshot) {
                        StudyMaterial material = document.toObject(StudyMaterial.class);
                        if (material != null) {
                            material.setId(document.getId());
                            
                            // Ensure likes field is properly initialized
                            Long likesLong = document.getLong("likes");
                            if (likesLong != null) {
                                material.setLikes(likesLong.intValue());
                            } else {
                                material.setLikes(0);
                            }
                            
                            // Ensure commentCount field is properly initialized
                            Long commentCountLong = document.getLong("commentCount");
                            if (commentCountLong != null) {
                                material.setCommentCount(commentCountLong.intValue());
                            } else {
                                material.setCommentCount(0);
                            }
                            
                            materials.add(material);
                        }
                    }
                    
                    // Check favorite status for each material
                    checkFavoriteStatusForMaterials(materials, callback);
                })
                .addOnFailureListener(callback::onFailure);
    }
    
    // Check favorite status for a list of materials
    private void checkFavoriteStatusForMaterials(List<StudyMaterial> materials, StudyMaterialCallback callback) {
        String currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        
        if (currentUserId == null || materials.isEmpty()) {
            callback.onSuccess(materials);
            return;
        }
        
        // Get all user favorites in a single query
        firestore.collection("user_favorites")
                .whereEqualTo("userId", currentUserId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    // Create a set of favorited material IDs for fast lookup
                    java.util.Set<String> favoritedIds = new java.util.HashSet<>();
                    for (DocumentSnapshot document : querySnapshot) {
                        String materialId = document.getString("materialId");
                        if (materialId != null) {
                            favoritedIds.add(materialId);
                        }
                    }
                    
                    // Update materials with their favorite status
                    for (StudyMaterial material : materials) {
                        material.setLikedByUser(favoritedIds.contains(material.getId()));
                    }
                    
                    callback.onSuccess(materials);
                })
                .addOnFailureListener(e -> {
                    // If query fails, return materials without favorite status
                    callback.onSuccess(materials);
                });
    }
    
    // Get study materials by category
    public void getStudyMaterialsByCategory(String category, StudyMaterialCallback callback) {
        firestore.collection("study_materials")
                .whereEqualTo("category", category)
                .orderBy("uploadDate", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<StudyMaterial> materials = new ArrayList<>();
                    for (DocumentSnapshot document : querySnapshot) {
                        StudyMaterial material = document.toObject(StudyMaterial.class);
                        if (material != null) {
                            material.setId(document.getId());
                            
                            // Ensure likes field is properly initialized
                            Long likesLong = document.getLong("likes");
                            if (likesLong != null) {
                                material.setLikes(likesLong.intValue());
                            } else {
                                material.setLikes(0);
                            }
                            
                            // Ensure commentCount field is properly initialized
                            Long commentCountLong = document.getLong("commentCount");
                            if (commentCountLong != null) {
                                material.setCommentCount(commentCountLong.intValue());
                            } else {
                                material.setCommentCount(0);
                            }
                            
                            materials.add(material);
                        }
                    }
                    
                    // Check favorite status for filtered materials
                    checkFavoriteStatusForMaterials(materials, callback);
                })
                .addOnFailureListener(callback::onFailure);
    }
    
    // Get study materials uploaded by specific user
    public void getStudyMaterialsByUser(String userId, StudyMaterialCallback callback) {
        firestore.collection("study_materials")
                .whereEqualTo("authorId", userId)
                .orderBy("uploadDate", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<StudyMaterial> materials = new ArrayList<>();
                    for (DocumentSnapshot document : querySnapshot) {
                        StudyMaterial material = document.toObject(StudyMaterial.class);
                        if (material != null) {
                            material.setId(document.getId());
                            
                            // Ensure likes field is properly initialized
                            Long likesLong = document.getLong("likes");
                            if (likesLong != null) {
                                material.setLikes(likesLong.intValue());
                            } else {
                                material.setLikes(0);
                            }
                            
                            // Ensure commentCount field is properly initialized
                            Long commentCountLong = document.getLong("commentCount");
                            if (commentCountLong != null) {
                                material.setCommentCount(commentCountLong.intValue());
                            } else {
                                material.setCommentCount(0);
                            }
                            
                            materials.add(material);
                        }
                    }
                    
                    // Check favorite status for user's materials
                    checkFavoriteStatusForMaterials(materials, callback);
                })
                .addOnFailureListener(callback::onFailure);
    }
    
    public void getDownloadedMaterialsByUser(String userId, StudyMaterialCallback callback) {
        // First get all materials that the user has downloaded
        firestore.collection("downloads")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(downloadSnapshot -> {
                    List<String> downloadedMaterialIds = new ArrayList<>();
                    for (DocumentSnapshot document : downloadSnapshot) {
                        String materialId = document.getString("materialId");
                        if (materialId != null) {
                            downloadedMaterialIds.add(materialId);
                        }
                    }
                    
                    if (downloadedMaterialIds.isEmpty()) {
                        callback.onSuccess(new ArrayList<>());
                        return;
                    }
                    
                    // Then get the actual study materials
                    firestore.collection("study_materials")
                            .whereIn("id", downloadedMaterialIds)
                            .orderBy("uploadDate", com.google.firebase.firestore.Query.Direction.DESCENDING)
                            .get()
                            .addOnSuccessListener(materialsSnapshot -> {
                                List<StudyMaterial> materials = new ArrayList<>();
                                for (DocumentSnapshot document : materialsSnapshot) {
                                    StudyMaterial material = document.toObject(StudyMaterial.class);
                                    if (material != null) {
                                        material.setId(document.getId());
                                        
                                        // Ensure likes field is properly initialized
                                        Long likesLong = document.getLong("likes");
                                        if (likesLong != null) {
                                            material.setLikes(likesLong.intValue());
                                        } else {
                                            material.setLikes(0);
                                        }
                                        
                                        // Ensure commentCount field is properly initialized
                                        Long commentCountLong = document.getLong("commentCount");
                                        if (commentCountLong != null) {
                                            material.setCommentCount(commentCountLong.intValue());
                                        } else {
                                            material.setCommentCount(0);
                                        }
                                        
                                        materials.add(material);
                                    }
                                }
                                checkFavoriteStatusForMaterials(materials, callback);
                            })
                            .addOnFailureListener(callback::onFailure);
                })
                .addOnFailureListener(callback::onFailure);
    }
    
    // Download study material
    public void downloadStudyMaterial(String fileUrl, DownloadCallback callback) {
        StorageReference storageRef = storage.getReferenceFromUrl(fileUrl);
        storageRef.getDownloadUrl()
                .addOnSuccessListener(callback::onSuccess)
                .addOnFailureListener(callback::onFailure);
    }
    
    // Increment download count
    public void incrementDownloadCount(String materialId, IncrementCallback callback) {
        Log.d(TAG, "Attempting to increment download count for material: " + materialId);
        
        // Try to update the main document first (should work with new rules)
        firestore.collection("study_materials")
                .document(materialId)
                .update("downloads", com.google.firebase.firestore.FieldValue.increment(1))
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Successfully incremented download count for material: " + materialId);
                    if (callback != null) {
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Failed to update main document download count, creating tracking instead: " + materialId);
                    // Fallback: Create tracking document
                    createDownloadTracking(materialId, callback);
                });
    }
    
    // Create download tracking as fallback
    private void createDownloadTracking(String materialId, IncrementCallback callback) {
        String trackingId = materialId + "_downloads_" + System.currentTimeMillis();
        Map<String, Object> trackingData = new HashMap<>();
        trackingData.put("materialId", materialId);
        trackingData.put("timestamp", System.currentTimeMillis());
        trackingData.put("userId", auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "anonymous");
        trackingData.put("type", "download");
        
        firestore.collection("view_download_tracking")
                .document(trackingId)
                .set(trackingData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Successfully created download tracking for material: " + materialId);
                    if (callback != null) {
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating download tracking for material: " + materialId, e);
                    if (callback != null) {
                        callback.onFailure(e);
                    }
                });
    }
    
    // Overloaded method for backward compatibility
    public void incrementDownloadCount(String materialId) {
        incrementDownloadCount(materialId, null);
    }
    
    // Increment view count
    public void incrementViewCount(String materialId, IncrementCallback callback) {
        Log.d(TAG, "Attempting to increment view count for material: " + materialId);
        
        // Try to update the main document first (should work with new rules)
        firestore.collection("study_materials")
                .document(materialId)
                .update("views", com.google.firebase.firestore.FieldValue.increment(1))
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Successfully incremented view count for material: " + materialId);
                    if (callback != null) {
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Failed to update main document view count, creating tracking instead: " + materialId);
                    // Fallback: Create tracking document
                    createViewTracking(materialId, callback);
                });
    }
    
    // Create view tracking as fallback
    private void createViewTracking(String materialId, IncrementCallback callback) {
        String trackingId = materialId + "_views_" + System.currentTimeMillis();
        Map<String, Object> trackingData = new HashMap<>();
        trackingData.put("materialId", materialId);
        trackingData.put("timestamp", System.currentTimeMillis());
        trackingData.put("userId", auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "anonymous");
        trackingData.put("type", "view");
        
        firestore.collection("view_download_tracking")
                .document(trackingId)
                .set(trackingData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Successfully created view tracking for material: " + materialId);
                    if (callback != null) {
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating view tracking for material: " + materialId, e);
                    if (callback != null) {
                        callback.onFailure(e);
                    }
                });
    }
    
    // Overloaded method for backward compatibility
    public void incrementViewCount(String materialId) {
        incrementViewCount(materialId, null);
    }
    
    public interface IncrementCallback {
        void onSuccess();
        void onFailure(Exception e);
    }
    
    // Get current download count for debugging
    public void getDownloadCount(String materialId, DownloadCountCallback callback) {
        firestore.collection("study_materials")
                .document(materialId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        StudyMaterial material = documentSnapshot.toObject(StudyMaterial.class);
                        if (material != null) {
                            Log.d(TAG, "Current download count for " + materialId + ": " + material.getDownloads());
                            callback.onSuccess(material.getDownloads());
                        } else {
                            callback.onFailure(new Exception("Failed to parse material"));
                        }
                    } else {
                        callback.onFailure(new Exception("Material not found"));
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }
    
    public interface DownloadCountCallback {
        void onSuccess(int downloadCount);
        void onFailure(Exception e);
    }
    
    // Like/Unlike study material
    public void toggleLike(String materialId, String userId, boolean isLiked) {
        Map<String, Object> update = new HashMap<>();
        if (isLiked) {
            update.put("likes", com.google.firebase.firestore.FieldValue.increment(1));
            update.put("likedBy." + userId, true);
        } else {
            update.put("likes", com.google.firebase.firestore.FieldValue.increment(-1));
            update.put("likedBy." + userId, com.google.firebase.firestore.FieldValue.delete());
        }
        
        firestore.collection("study_materials")
                .document(materialId)
                .update(update)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Like count updated successfully for material: " + materialId);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error toggling like", e));
    }
    
    // Get updated counts for a material
    public void getMaterialCounts(String materialId, MaterialCountsCallback callback) {
        firestore.collection("study_materials")
                .document(materialId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        int likes = documentSnapshot.getLong("likes") != null ? 
                                   documentSnapshot.getLong("likes").intValue() : 0;
                        int commentCount = documentSnapshot.getLong("commentCount") != null ? 
                                         documentSnapshot.getLong("commentCount").intValue() : 0;
                        
                        callback.onSuccess(likes, commentCount);
                    } else {
                        callback.onSuccess(0, 0);
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }
    
    public interface MaterialCountsCallback {
        void onSuccess(int likes, int commentCount);
        void onFailure(Exception e);
    }
    
    // Delete study material from Storage and Firestore
    public void deleteStudyMaterial(String materialId, String fileName, DeleteCallback callback) {
        // Delete from Storage
        StorageReference storageRef = storage.getReference().child(fileName);
        storageRef.delete().addOnSuccessListener(aVoid -> {
            // Delete from Firestore
            firestore.collection("study_materials")
                    .document(materialId)
                    .delete()
                    .addOnSuccessListener(aVoid2 -> {
                        // Delete related data (comments, ratings, favorites)
                        deleteRelatedData(materialId, callback);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error deleting from Firestore", e);
                        // Even if Firestore deletion fails, we should still call success
                        // since the file was deleted from storage
                        callback.onSuccess();
                    });
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error deleting from Storage", e);
            
            // Check if it's a permission error
            if (e instanceof com.google.firebase.storage.StorageException) {
                com.google.firebase.storage.StorageException storageException = (com.google.firebase.storage.StorageException) e;
                // Check for permission denied error (error code -13021)
                if (storageException.getErrorCode() == -13021) {
                    Log.w(TAG, "Permission denied for storage deletion, proceeding with Firestore deletion only");
                    
                    // Even if storage deletion fails due to permissions, 
                    // we can still delete from Firestore and related data
                    firestore.collection("study_materials")
                            .document(materialId)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                deleteRelatedData(materialId, callback);
                            })
                            .addOnFailureListener(firestoreError -> {
                                Log.e(TAG, "Error deleting from Firestore after storage permission error", firestoreError);
                                callback.onSuccess(); // Call success anyway to update UI
                            });
                } else {
                    // For other storage errors, still try to delete from Firestore
                    firestore.collection("study_materials")
                            .document(materialId)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                deleteRelatedData(materialId, callback);
                            })
                            .addOnFailureListener(firestoreError -> {
                                Log.e(TAG, "Error deleting from Firestore", firestoreError);
                                callback.onSuccess(); // Call success anyway to update UI
                            });
                }
            } else {
                // For non-storage exceptions, still try to delete from Firestore
                firestore.collection("study_materials")
                        .document(materialId)
                        .delete()
                        .addOnSuccessListener(aVoid -> {
                            deleteRelatedData(materialId, callback);
                        })
                        .addOnFailureListener(firestoreError -> {
                            Log.e(TAG, "Error deleting from Firestore", firestoreError);
                            callback.onSuccess(); // Call success anyway to update UI
                        });
            }
        });
    }

    // Overloaded method for backward compatibility
    public void deleteStudyMaterial(String materialId, String fileName, Runnable onSuccess) {
        deleteStudyMaterial(materialId, fileName, new DeleteCallback() {
            @Override
            public void onSuccess() {
                onSuccess.run();
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Delete operation failed", e);
                // Still call success to update UI
                onSuccess.run();
            }
        });
    }

    // Delete related data (comments, ratings, favorites)
    private void deleteRelatedData(String materialId, DeleteCallback callback) {
        // Delete comments
        firestore.collection("comments")
                .whereEqualTo("materialId", materialId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot document : querySnapshot) {
                        document.getReference().delete();
                    }
                });

        // Delete ratings
        firestore.collection("document_ratings")
                .whereEqualTo("materialId", materialId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot document : querySnapshot) {
                        document.getReference().delete();
                    }
                });

        // Delete favorites
        firestore.collection("user_favorites")
                .whereEqualTo("materialId", materialId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot document : querySnapshot) {
                        document.getReference().delete();
                    }
                    // Call success callback after all deletions
                    callback.onSuccess();
                });
    }
    
    // Search study materials by title or description
    public void searchStudyMaterials(String query, StudyMaterialCallback callback) {
        // Convert query to lowercase for case-insensitive search
        String searchQuery = query.toLowerCase();
        
        firestore.collection("study_materials")
                .orderBy("uploadDate", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<StudyMaterial> materials = new ArrayList<>();
                    for (DocumentSnapshot document : querySnapshot) {
                        StudyMaterial material = document.toObject(StudyMaterial.class);
                        if (material != null) {
                            material.setId(document.getId());
                            
                            // Ensure likes field is properly initialized
                            Long likesLong = document.getLong("likes");
                            if (likesLong != null) {
                                material.setLikes(likesLong.intValue());
                            } else {
                                material.setLikes(0);
                            }
                            
                            // Ensure commentCount field is properly initialized
                            Long commentCountLong = document.getLong("commentCount");
                            if (commentCountLong != null) {
                                material.setCommentCount(commentCountLong.intValue());
                            } else {
                                material.setCommentCount(0);
                            }
                            
                            // Check if title or description contains the search query
                            String title = material.getTitle().toLowerCase();
                            String description = material.getDescription().toLowerCase();
                            
                            if (title.contains(searchQuery) || description.contains(searchQuery)) {
                                materials.add(material);
                            }
                        }
                    }
                    callback.onSuccess(materials);
                })
                .addOnFailureListener(callback::onFailure);
    }
    
    // Search study materials by title or description within a specific category
    public void searchStudyMaterialsByCategory(String query, String category, StudyMaterialCallback callback) {
        // Convert query to lowercase for case-insensitive search
        String searchQuery = query.toLowerCase();
        
        firestore.collection("study_materials")
                .whereEqualTo("category", category)
                .orderBy("uploadDate", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<StudyMaterial> materials = new ArrayList<>();
                    for (DocumentSnapshot document : querySnapshot) {
                        StudyMaterial material = document.toObject(StudyMaterial.class);
                        if (material != null) {
                            material.setId(document.getId());
                            
                            // Ensure likes field is properly initialized
                            Long likesLong = document.getLong("likes");
                            if (likesLong != null) {
                                material.setLikes(likesLong.intValue());
                            } else {
                                material.setLikes(0);
                            }
                            
                            // Ensure commentCount field is properly initialized
                            Long commentCountLong = document.getLong("commentCount");
                            if (commentCountLong != null) {
                                material.setCommentCount(commentCountLong.intValue());
                            } else {
                                material.setCommentCount(0);
                            }
                            
                            // Check if title or description contains the search query
                            String title = material.getTitle().toLowerCase();
                            String description = material.getDescription().toLowerCase();
                            
                            if (title.contains(searchQuery) || description.contains(searchQuery)) {
                                materials.add(material);
                            }
                        }
                    }
                    callback.onSuccess(materials);
                })
                .addOnFailureListener(callback::onFailure);
    }
    
    // Search study materials by title or description for a specific user
    public void searchStudyMaterialsByUser(String userId, String query, StudyMaterialCallback callback) {
        // Convert query to lowercase for case-insensitive search
        String searchQuery = query.toLowerCase();
        
        firestore.collection("study_materials")
                .whereEqualTo("authorId", userId)
                .orderBy("uploadDate", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<StudyMaterial> materials = new ArrayList<>();
                    for (DocumentSnapshot document : querySnapshot) {
                        StudyMaterial material = document.toObject(StudyMaterial.class);
                        if (material != null) {
                            material.setId(document.getId());
                            
                            // Ensure likes field is properly initialized
                            Long likesLong = document.getLong("likes");
                            if (likesLong != null) {
                                material.setLikes(likesLong.intValue());
                            } else {
                                material.setLikes(0);
                            }
                            
                            // Ensure commentCount field is properly initialized
                            Long commentCountLong = document.getLong("commentCount");
                            if (commentCountLong != null) {
                                material.setCommentCount(commentCountLong.intValue());
                            } else {
                                material.setCommentCount(0);
                            }
                            
                            // Check if title or description contains the search query
                            String title = material.getTitle().toLowerCase();
                            String description = material.getDescription().toLowerCase();
                            
                            if (title.contains(searchQuery) || description.contains(searchQuery)) {
                                materials.add(material);
                            }
                        }
                    }
                    callback.onSuccess(materials);
                })
                .addOnFailureListener(callback::onFailure);
    }
    
    // Search user by username
    public void searchUserByUsername(String username, UserSearchCallback callback) {
        firestore.collection("users")
                .whereEqualTo("username", username)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                        com.example.nurse_connect.models.User user = document.toObject(com.example.nurse_connect.models.User.class);
                        if (user != null) {
                            user.setUid(document.getId());
                            callback.onSuccess(user);
                        } else {
                            callback.onFailure(new Exception("User not found"));
                        }
                    } else {
                        callback.onFailure(new Exception("User not found"));
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }
    
    public interface UserSearchCallback {
        void onSuccess(com.example.nurse_connect.models.User user);
        void onFailure(Exception e);
    }

    public interface UpdateCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    public interface DeleteCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    // Update study material details
    public void updateStudyMaterial(String materialId, StudyMaterial material, UpdateCallback callback) {
        // Create a map with the updated fields
        Map<String, Object> updates = new HashMap<>();
        updates.put("title", material.getTitle());
        updates.put("description", material.getDescription());
        updates.put("category", material.getCategory());
        updates.put("type", material.getType());
        updates.put("price", material.getPrice());
        updates.put("privacy", material.getPrivacy());
        updates.put("lastModified", System.currentTimeMillis());

        firestore.collection("study_materials")
                .document(materialId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    callback.onSuccess();
                })
                .addOnFailureListener(callback::onFailure);
    }
    
    // Calculate user achievements from their study materials
    public void calculateUserAchievements(String userId, UserAchievementsCallback callback) {
        firestore.collection("study_materials")
                .whereEqualTo("authorId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int totalMaterials = 0;
                    int totalDownloads = 0;
                    int totalLikes = 0;
                    float totalRating = 0.0f;
                    int materialsWithRating = 0;
                    
                    for (DocumentSnapshot document : querySnapshot) {
                        StudyMaterial material = document.toObject(StudyMaterial.class);
                        if (material != null) {
                            totalMaterials++;
                            totalDownloads += material.getDownloads();
                            totalLikes += material.getLikes();
                            
                            if (material.getRating() > 0) {
                                totalRating += material.getRating();
                                materialsWithRating++;
                            }
                        }
                    }
                    
                    float averageRating = materialsWithRating > 0 ? totalRating / materialsWithRating : 0.0f;
                    
                    UserAchievements achievements = new UserAchievements();
                    achievements.setTotalMaterials(totalMaterials);
                    achievements.setTotalDownloads(totalDownloads);
                    achievements.setTotalLikes(totalLikes);
                    achievements.setAverageRating(averageRating);
                    
                    callback.onSuccess(achievements);
                })
                .addOnFailureListener(callback::onFailure);
    }
    
    public interface UserAchievementsCallback {
        void onSuccess(UserAchievements achievements);
        void onFailure(Exception e);
    }
    
    // Inner class to hold user achievements data
    public static class UserAchievements {
        private int totalMaterials = 0;
        private int totalDownloads = 0;
        private int totalLikes = 0;
        private float averageRating = 0.0f;
        
        public UserAchievements() {}
        
        public int getTotalMaterials() { return totalMaterials; }
        public void setTotalMaterials(int totalMaterials) { this.totalMaterials = totalMaterials; }
        
        public int getTotalDownloads() { return totalDownloads; }
        public void setTotalDownloads(int totalDownloads) { this.totalDownloads = totalDownloads; }
        
        public int getTotalLikes() { return totalLikes; }
        public void setTotalLikes(int totalLikes) { this.totalLikes = totalLikes; }
        
        public float getAverageRating() { return averageRating; }
        public void setAverageRating(float averageRating) { this.averageRating = averageRating; }
    }
    
    // Generate and upload thumbnail
    private void generateAndUploadThumbnail(Context context, Uri pdfUri, ThumbnailCallback callback) {
        Log.d(TAG, "Starting thumbnail generation for PDF: " + pdfUri);
        try {
            // Generate thumbnail
            Bitmap thumbnail = PdfThumbnailGenerator.generateThumbnail(context, pdfUri);
            if (thumbnail == null) {
                Log.w(TAG, "Failed to generate thumbnail, continuing without thumbnail");
                callback.onSuccess(null);
                return;
            }
            
            Log.d(TAG, "Thumbnail generated successfully, size: " + thumbnail.getWidth() + "x" + thumbnail.getHeight());
            
            // Convert bitmap to byte array
            byte[] thumbnailBytes = PdfThumbnailGenerator.bitmapToByteArray(thumbnail);
            if (thumbnailBytes == null) {
                Log.w(TAG, "Failed to convert thumbnail to bytes, continuing without thumbnail");
                callback.onSuccess(null);
                return;
            }
            
            // Upload thumbnail to Firebase Storage
            String thumbnailFileName = "thumbnails/" + UUID.randomUUID().toString() + ".jpg";
            StorageReference thumbnailRef = storage.getReference().child(thumbnailFileName);
            
            UploadTask thumbnailUploadTask = thumbnailRef.putBytes(thumbnailBytes);
            
            thumbnailUploadTask.addOnSuccessListener(taskSnapshot -> {
                Log.d(TAG, "Thumbnail uploaded successfully to Firebase Storage");
                // Get thumbnail download URL
                thumbnailRef.getDownloadUrl().addOnSuccessListener(thumbnailUri -> {
                    Log.d(TAG, "Thumbnail download URL obtained: " + thumbnailUri.toString());
                    callback.onSuccess(thumbnailUri.toString());
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get thumbnail download URL: " + e.getMessage(), e);
                    callback.onSuccess(null); // Continue without thumbnail
                });
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Failed to upload thumbnail: " + e.getMessage(), e);
                callback.onSuccess(null); // Continue without thumbnail
            });
            
            // Clean up bitmap
            thumbnail.recycle();
            
        } catch (Exception e) {
            Log.e(TAG, "Error generating/uploading thumbnail: " + e.getMessage(), e);
            callback.onSuccess(null); // Continue without thumbnail
        }
    }
} 