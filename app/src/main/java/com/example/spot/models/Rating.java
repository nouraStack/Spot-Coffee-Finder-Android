package com.example.spot.models;

public class Rating {
    private String ratingId;
    private String userId;
    private String userName;
    private String cafeId;
    private float score;
    private String comment;
    private long timestamp;

    public Rating() {}

    public Rating(String ratingId, String userId, String userName, String cafeId,
                  float score, String comment) {
        this.ratingId = ratingId;
        this.userId = userId;
        this.userName = userName;
        this.cafeId = cafeId;
        this.score = score;
        this.comment = comment;
        this.timestamp = System.currentTimeMillis();
    }

    public String getRatingId() { return ratingId; }
    public void setRatingId(String ratingId) { this.ratingId = ratingId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getCafeId() { return cafeId; }
    public void setCafeId(String cafeId) { this.cafeId = cafeId; }
    public float getScore() { return score; }
    public void setScore(float score) { this.score = score; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}

