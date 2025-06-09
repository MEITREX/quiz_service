package de.unistuttgart.iste.meitrex.quiz_service.controller;


import de.unistuttgart.iste.meitrex.quiz_service.persistence.entity.QuestionEntity;
import de.unistuttgart.iste.meitrex.quiz_service.persistence.entity.QuizEntity;
import de.unistuttgart.iste.meitrex.quiz_service.service.AiQuizGenerationService;
import de.unistuttgart.iste.meitrex.quiz_service.service.QuizService;

import de.unistuttgart.iste.meitrex.common.exception.NoAccessToCourseException;
import de.unistuttgart.iste.meitrex.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.meitrex.common.user_handling.LoggedInUser.UserRoleInCourse;
import de.unistuttgart.iste.meitrex.generated.dto.*;

import de.unistuttgart.iste.meitrex.quiz_service.service.model.AiQuizGenLimits;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.graphql.data.method.annotation.*;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static de.unistuttgart.iste.meitrex.common.user_handling.UserCourseAccessValidator.validateUserHasAccessToCourse;

@Slf4j
@Controller
@RequiredArgsConstructor
public class QuizController {

    private static final String QUIZ_MUTATION_NAME = "QuizMutation";

    private final QuizService quizService;
    private final AiQuizGenerationService aiQuizGenerationService;

    @QueryMapping
    public List<Quiz> findQuizzesByAssessmentIds(@Argument final List<UUID> assessmentIds,
                                                 @ContextValue final LoggedInUser currentUser) {
        return quizService.findQuizzesByAssessmentIds(assessmentIds).stream()
                .map(quiz -> {
                    if (quiz == null) {
                        return null;
                    }
                    try {
                        validateUserHasAccessToCourse(currentUser, UserRoleInCourse.STUDENT, quiz.getCourseId());
                        return quiz;
                    } catch (final NoAccessToCourseException ex) {
                        return null;
                    }
                })
                .toList();
    }

    @MutationMapping(name = "_internal_noauth_createQuiz")
    public Quiz internalNoAuthCreateQuiz(@Argument final UUID courseId,
                                         @Argument final UUID assessmentId,
                                         @Argument final CreateQuizInput input) {
        return quizService.createQuiz(courseId, assessmentId, input);
    }

    @MutationMapping
    public QuizMutation mutateQuiz(@Argument final UUID assessmentId,
                                   @ContextValue final LoggedInUser currentUser) {
        final QuizEntity quiz = quizService.requireQuizExists(assessmentId);

        validateUserHasAccessToCourse(currentUser, UserRoleInCourse.ADMINISTRATOR, quiz.getCourseId());

        // this is basically an empty object, only serving as a parent for the nested mutations
        return new QuizMutation(assessmentId);
    }

    @MutationMapping
    public UUID deleteQuiz(@Argument final UUID assessmentId) {
        return quizService.deleteQuiz(assessmentId);
    }

    @SchemaMapping(typeName = QUIZ_MUTATION_NAME)
    public Quiz _internal_noauth_addMultipleChoiceQuestion(@Argument final CreateMultipleChoiceQuestionInput input, final QuizMutation quizMutation) {
        return quizService.addMultipleChoiceQuestion(quizMutation.getAssessmentId(), input);
    }

    @SchemaMapping(typeName = QUIZ_MUTATION_NAME)
    public Quiz _internal_noauth_updateMultipleChoiceQuestion(@Argument final UpdateMultipleChoiceQuestionInput input, final QuizMutation quizMutation) {
        return quizService.updateMultipleChoiceQuestion(quizMutation.getAssessmentId(), input);
    }

    @SchemaMapping(typeName = QUIZ_MUTATION_NAME)
    public Quiz _internal_noauth_addClozeQuestion(@Argument final CreateClozeQuestionInput input, final QuizMutation quizMutation) {
        return quizService.addClozeQuestion(quizMutation.getAssessmentId(), input);
    }

    @SchemaMapping(typeName = QUIZ_MUTATION_NAME)
    public Quiz _internal_noauth_updateClozeQuestion(@Argument final UpdateClozeQuestionInput input, final QuizMutation quizMutation) {
        return quizService.updateClozeQuestion(quizMutation.getAssessmentId(), input);
    }

    @SchemaMapping(typeName = QUIZ_MUTATION_NAME)
    public Quiz _internal_noauth_addAssociationQuestion(@Argument final CreateAssociationQuestionInput input, final QuizMutation quizMutation) {
        return quizService.addAssociationQuestion(quizMutation.getAssessmentId(), input);
    }

    @SchemaMapping(typeName = QUIZ_MUTATION_NAME)
    public Quiz _internal_noauth_updateAssociationQuestion(@Argument final UpdateAssociationQuestionInput input, final QuizMutation quizMutation) {
        return quizService.updateAssociationQuestion(quizMutation.getAssessmentId(), input);
    }

    @SchemaMapping(typeName = QUIZ_MUTATION_NAME)
    public Quiz _internal_noauth_addExactAnswerQuestion(@Argument final CreateExactAnswerQuestionInput input, final QuizMutation quizMutation) {
        return quizService.addExactAnswerQuestion(quizMutation.getAssessmentId(), input);
    }

    @SchemaMapping(typeName = QUIZ_MUTATION_NAME)
    public Quiz _internal_noauth_updateExactAnswerQuestion(@Argument final UpdateExactAnswerQuestionInput input, final QuizMutation quizMutation) {
        return quizService.updateExactAnswerQuestion(quizMutation.getAssessmentId(), input);
    }

    @SchemaMapping(typeName = QUIZ_MUTATION_NAME)
    public Quiz _internal_noauth_addNumericQuestion(@Argument final CreateNumericQuestionInput input, final QuizMutation quizMutation) {
        return quizService.addNumericQuestion(quizMutation.getAssessmentId(), input);
    }

    @SchemaMapping(typeName = QUIZ_MUTATION_NAME)
    public Quiz _internal_noauth_updateNumericQuestion(@Argument final UpdateNumericQuestionInput input, final QuizMutation quizMutation) {
        return quizService.updateNumericQuestion(quizMutation.getAssessmentId(), input);
    }

    @SchemaMapping(typeName = QUIZ_MUTATION_NAME)
    public Quiz _internal_noauth_addSelfAssessmentQuestion(@Argument final CreateSelfAssessmentQuestionInput input, final QuizMutation quizMutation) {
        return quizService.addSelfAssessmentQuestion(quizMutation.getAssessmentId(), input);
    }

    @SchemaMapping(typeName = QUIZ_MUTATION_NAME)
    public Quiz _internal_noauth_updateSelfAssessmentQuestion(@Argument final UpdateSelfAssessmentQuestionInput input, final QuizMutation quizMutation) {
        return quizService.updateSelfAssessmentQuestion(quizMutation.getAssessmentId(), input);
    }

    @SchemaMapping(typeName = QUIZ_MUTATION_NAME)
    public Quiz removeQuestion(@Argument final int number, final QuizMutation quizMutation) {
        return quizService.removeQuestion(quizMutation.getAssessmentId(), number);
    }

    @SchemaMapping(typeName = QUIZ_MUTATION_NAME)
    public Quiz switchQuestions(@Argument final int firstNumber, @Argument final int secondNumber, final QuizMutation quizMutation) {
        return quizService.switchQuestions(quizMutation.getAssessmentId(), firstNumber, secondNumber);
    }

    @SchemaMapping(typeName = QUIZ_MUTATION_NAME)
    public Quiz setRequiredCorrectAnswers(@Argument final int requiredCorrectAnswers, final QuizMutation quizMutation) {
        return quizService.setRequiredCorrectAnswers(quizMutation.getAssessmentId(), requiredCorrectAnswers);
    }

    @SchemaMapping(typeName = QUIZ_MUTATION_NAME)
    public Quiz setQuestionPoolingMode(@Argument final QuestionPoolingMode questionPoolingMode, final QuizMutation quizMutation) {
        return quizService.setQuestionPoolingMode(quizMutation.getAssessmentId(), questionPoolingMode);
    }

    @SchemaMapping(typeName = QUIZ_MUTATION_NAME)
    public Quiz setNumberOfRandomlySelectedQuestions(@Argument final int numberOfRandomlySelectedQuestions, final QuizMutation quizMutation) {
        return quizService.setNumberOfRandomlySelectedQuestions(quizMutation.getAssessmentId(), numberOfRandomlySelectedQuestions);
    }

    @MutationMapping
    public QuizCompletionFeedback logQuizCompleted(@Argument final QuizCompletedInput input,
                                                   @ContextValue final LoggedInUser currentUser) {
        final QuizEntity quiz = quizService.requireQuizExists(input.getQuizId());

        validateUserHasAccessToCourse(currentUser, UserRoleInCourse.STUDENT, quiz.getCourseId());

        return quizService.publishProgress(input, currentUser.getId());
    }

    @SchemaMapping(value = "aiGenerateQuestionAsync", typeName = QUIZ_MUTATION_NAME)
    public QuizAIGenAsyncResponse aiGenerateQuestions(@Argument final AiGenQuestionContext context, @ContextValue final LoggedInUser currentUser, final QuizMutation quizMutation) {
        final QuizEntity q = quizService.requireQuizExists(quizMutation.getAssessmentId());
        validateUserHasAccessToCourse(currentUser, UserRoleInCourse.STUDENT, q.getCourseId());
        Optional.of(q).map(
                    quiz -> Mono
                            .fromRunnable(() -> backgroundQuestionGeneration(context, quizMutation))
                            .subscribeOn(Schedulers.boundedElastic())
                            .mapNotNull(r -> quizService.findQuizById(q.getAssessmentId()).orElse(null))
        ).orElseGet(Mono::empty)
                .subscribe();
        Quiz quiz = quizService.findQuizById(q.getAssessmentId()).orElse(null);
        QuizAIGenAsyncResponse res = new QuizAIGenAsyncResponse();
        res.setQuiz(quiz);
        return res;
    }

    // runs the background task to generate and add questions to a quiz
    protected void backgroundQuestionGeneration(AiGenQuestionContext context, final QuizMutation quizMutation) {
        final String description = context.getDescription();
        final List<String> mediaRecordIds = context.getMediaRecordIds() == null ? List.of() : context.getMediaRecordIds().stream().map(UUID::toString).toList();
        AiQuizGenLimits limits = getAiQuizGenLimits(context);
        final QuizEntity q = quizService.requireQuizExists(quizMutation.getAssessmentId());
        try {
            aiQuizGenerationService.fillQuizWithQuestions(q, limits, description, mediaRecordIds);
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    @NotNull
    private static AiQuizGenLimits getAiQuizGenLimits(AiGenQuestionContext context) {
        AiQuizGenLimits limits = new AiQuizGenLimits();
        limits.setMaxExactQuestions(context.getMaxExactQuestions());
        limits.setMaxMultipleChoiceQuestions(context.getMaxMultipleChoiceQuestions());
        limits.setMaxQuestions(context.getMaxQuestions());
        limits.setMinQuestions(context.getMinQuestions());
        limits.setMaxFreeTextQuestions(context.getMaxFreeTextQuestions());
        limits.setMaxNumericQuestions(context.getMaxNumericQuestions());
        limits.setAllowMultipleCorrectAnswers(context.getAllowMultipleCorrectAnswers());
        limits.setMaxAnswersPerQuestion(context.getMaxAnswersPerQuestion());
        return limits;
    }

}
