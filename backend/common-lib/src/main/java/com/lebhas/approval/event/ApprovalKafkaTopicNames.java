package com.lebhas.approval.event;

import com.lebhas.creativesaas.messaging.kafka.KafkaTopicConstants;
import org.springframework.util.StringUtils;

public class ApprovalKafkaTopicNames {

    private final String topicPrefix;

    public ApprovalKafkaTopicNames(String topicPrefix) {
        this.topicPrefix = topicPrefix;
    }

    public String approvalRequestSubmitted() {
        return resolve(KafkaTopicConstants.APPROVAL_REQUEST_SUBMITTED);
    }

    public String approvalReviewStarted() {
        return resolve(KafkaTopicConstants.APPROVAL_REVIEW_STARTED);
    }

    public String approvalApproved() {
        return resolve(KafkaTopicConstants.APPROVAL_APPROVED);
    }

    public String approvalRejected() {
        return resolve(KafkaTopicConstants.APPROVAL_REJECTED);
    }

    public String approvalChangesRequested() {
        return resolve(KafkaTopicConstants.APPROVAL_CHANGES_REQUESTED);
    }

    public String approvalResubmitted() {
        return resolve(KafkaTopicConstants.APPROVAL_RESUBMITTED);
    }

    public String approvalAssigned() {
        return resolve(KafkaTopicConstants.APPROVAL_ASSIGNED);
    }

    public String approvalCommentCreated() {
        return resolve(KafkaTopicConstants.APPROVAL_COMMENT_CREATED);
    }

    public String[] lifecycleTopics() {
        return new String[] {
                approvalRequestSubmitted(),
                approvalReviewStarted(),
                approvalApproved(),
                approvalRejected(),
                approvalChangesRequested(),
                approvalResubmitted(),
                approvalAssigned(),
                approvalCommentCreated()
        };
    }

    private String resolve(String topic) {
        if (!StringUtils.hasText(topicPrefix)) {
            return topic;
        }
        return topicPrefix + topic;
    }
}
