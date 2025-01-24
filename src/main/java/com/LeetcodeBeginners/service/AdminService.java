package com.LeetcodeBeginners.service;

import com.LeetcodeBeginners.dto.QuestionDTO;
import com.LeetcodeBeginners.dto.TopicDTO;
import com.LeetcodeBeginners.entity.Question;
import com.LeetcodeBeginners.entity.Topic;
import com.LeetcodeBeginners.exception.ResourceNotFoundException;
import com.LeetcodeBeginners.repository.QuestionRepository;
import com.LeetcodeBeginners.repository.TopicRepository;
import org.bson.types.ObjectId;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AdminService {

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private ModelMapper modelMapper;

    /**
     * Get all topics
     */
    public List<TopicDTO> getAllTopics() {
        List<Topic> topics = topicRepository.findAll();

        return topics.stream()
                .map(topic -> {
                    // Map Topic to TopicDTO
                    TopicDTO topicDTO = modelMapper.map(topic, TopicDTO.class);

                    // Convert ObjectId list to String list
                    List<String> questionIds = topic.getQuestionIds()
                            .stream()
                            .map(ObjectId::toString) // Convert ObjectId to String
                            .toList();

                    topicDTO.setQuestionIds(questionIds);
                    return topicDTO;
                })
                .toList();
    }

    /**
     * Create a new topic and initialize its question collection
     */
    public TopicDTO createTopic(TopicDTO topicDTO) {
        validateTopic(topicDTO);

        // Map DTO to entity
        Topic topic = modelMapper.map(topicDTO, Topic.class);

        // Initialize questionIds as an empty list
        topic.setQuestionIds(new ArrayList<>());

        // Save the topic
        Topic savedTopic = topicRepository.save(topic);

        // Convert the saved topic back to DTO
        TopicDTO savedTopicDTO = modelMapper.map(savedTopic, TopicDTO.class);

        // Ensure questionIds is set as empty in the DTO
        savedTopicDTO.setQuestionIds(new ArrayList<>());

        return savedTopicDTO;
    }

    /**
     * Update an existing topic
     */
    public TopicDTO updateTopic(String id, TopicDTO updatedTopicDTO) {
        validateTopic(updatedTopicDTO);

        Topic topic = topicRepository.findById(new ObjectId(id))
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found with ID: " + id));

        topic.setDataStructure(updatedTopicDTO.getDataStructure());
        Topic updatedTopic = topicRepository.save(topic);

        return modelMapper.map(updatedTopic, TopicDTO.class);
    }

    public TopicDTO getTopicById(String topicId) {
        Topic topic = topicRepository.findById(new ObjectId(topicId))
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found with ID: " + topicId));

        // Convert the Topic to TopicDTO
        TopicDTO topicDTO = modelMapper.map(topic, TopicDTO.class);
        List<String> questionIds = topic.getQuestionIds()
                .stream()
                .map(ObjectId::toString) // Convert ObjectId to String
                .toList();

        topicDTO.setQuestionIds(questionIds);
        return topicDTO;
    }


    /**
     * Delete a topic
     */
    public void deleteTopic(String id) {
        if (!topicRepository.existsById(new ObjectId(id))) {
            throw new ResourceNotFoundException("Topic not found with ID: " + id);
        }
        topicRepository.deleteById(new ObjectId(id));
    }

    /**
     * Get all questions for a specific topic
     */
    public List<QuestionDTO> getQuestionsByTopic(String topicId) {
        List<Question> questions = questionRepository.findByTopicId(new ObjectId(topicId));
        return questions.stream()
                .map(question -> modelMapper.map(question, QuestionDTO.class))
                .toList();
    }

    /**
     * Add a question to a topic
     */
    public QuestionDTO addQuestionToTopic(String topicId, QuestionDTO questionDTO) {
        validateQuestion(questionDTO);

        // Map DTO to entity
        Question question = modelMapper.map(questionDTO, Question.class);
        question.setQuestionId(new ObjectId());
        question.setTopicId(new ObjectId(topicId));

        // Save the question
        Question savedQuestion = questionRepository.save(question);

        // Update the topic with the new question ID
        Topic topic = topicRepository.findById(new ObjectId(topicId))
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found with ID: " + topicId));
        topic.getQuestionIds().add(savedQuestion.getQuestionId());
        topicRepository.save(topic);

        return modelMapper.map(savedQuestion, QuestionDTO.class);
    }


    /**
     * Update a question in a topic
     */
    public QuestionDTO updateQuestion(String questionId, QuestionDTO updatedQuestionDTO) {
        validateQuestion(updatedQuestionDTO);

        Question question = questionRepository.findById(new ObjectId(questionId))
                .orElseThrow(() -> new ResourceNotFoundException("Question not found with ID: " + questionId));

        question.setQuestionName(updatedQuestionDTO.getQuestionName());
        question.setUrl(updatedQuestionDTO.getUrl());
        question.setLevel(updatedQuestionDTO.getLevel());
        question.setDataStructure(updatedQuestionDTO.getDataStructure());

        Question updatedQuestion = questionRepository.save(question);
        return modelMapper.map(updatedQuestion, QuestionDTO.class);
    }

    /**
     * Delete a question
     */
    public void deleteQuestion(String topicId, String questionId) {
        Question question = questionRepository.findById(new ObjectId(questionId))
                .orElseThrow(() -> new ResourceNotFoundException("Question not found with ID: " + questionId));

        if (!question.getTopicId().toHexString().equals(topicId)) {
            throw new ResourceNotFoundException("Question does not belong to topic with ID: " + topicId);
        }

        questionRepository.deleteById(new ObjectId(questionId));
    }


    /**
     * Validate topic before saving or updating
     */
    private void validateTopic(TopicDTO topicDTO) {
        if (topicDTO.getDataStructure() == null || topicDTO.getDataStructure().isEmpty()) {
            throw new IllegalArgumentException("DataStructure cannot be null or empty");
        }
    }

    /**
     * Validate question before saving or updating
     */
    private void validateQuestion(QuestionDTO questionDTO) {
        if (questionDTO.getQuestionName() == null || questionDTO.getQuestionName().isEmpty()) {
            throw new IllegalArgumentException("Question name cannot be null or empty");
        }
        if (questionDTO.getUrl() == null || questionDTO.getUrl().isEmpty()) {
            throw new IllegalArgumentException("Question URL cannot be null or empty");
        }
        if (questionDTO.getLevel() == null || questionDTO.getLevel().isEmpty()) {
            throw new IllegalArgumentException("Question level cannot be null or empty");
        }
    }
}
