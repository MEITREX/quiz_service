package de.unistuttgart.iste.meitrex.quiz_service.controller;

import de.unistuttgart.iste.meitrex.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.meitrex.common.user_handling.UserCourseAccessValidator;
import de.unistuttgart.iste.meitrex.generated.dto.AiGenQuestionContext;
import de.unistuttgart.iste.meitrex.generated.dto.Quiz;
import de.unistuttgart.iste.meitrex.generated.dto.QuizMutation;
import de.unistuttgart.iste.meitrex.quiz_service.persistence.entity.QuizEntity;
import de.unistuttgart.iste.meitrex.quiz_service.service.AiQuizGenerationService;
import de.unistuttgart.iste.meitrex.quiz_service.service.QuizService;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

public class QuizControllerTest {

    QuizService quizService = Mockito.mock(QuizService.class);
    AiQuizGenerationService aiQuizGenerationService = Mockito.mock(AiQuizGenerationService.class);
    MockedStatic<UserCourseAccessValidator> userCourseAccessValidator = Mockito.mockStatic(UserCourseAccessValidator.class);
    QuizController quizController = new QuizController(quizService, aiQuizGenerationService);

    @Test
    public void aiGenerateQuestionsAsyncTest() throws InterruptedException {

        long delay = 5000L;
        Date now = new Date();

        when(aiQuizGenerationService.generateQuizQuestions(any(),any(), any())).thenAnswer(r -> {
            // Create a long running task
            Thread.sleep(delay);
            return List.of();
        });
        userCourseAccessValidator.when(() -> UserCourseAccessValidator.validateUserHasAccessToCourse(any(), any(),any())).then(r -> r);


        when(quizService.requireQuizExists(any(UUID.class))).thenAnswer(r -> {
            QuizEntity qe = new QuizEntity();
            qe.setAssessmentId(r.getArgument(0,UUID.class));
            return qe;
        });

        when(quizService.findQuizById(any(UUID.class))).thenAnswer(r -> {
            Quiz qe = new Quiz();
            qe.setAssessmentId(r.getArgument(0,UUID.class));
            return qe;
        });

        LoggedInUser loggedInUser = Mockito.mock(LoggedInUser.class);
        when(loggedInUser.getId()).thenReturn(UUID.randomUUID());
        AiGenQuestionContext context = new AiGenQuestionContext();
        context.setDescription("Test description");

        UUID quizId = UUID.randomUUID();

        QuizMutation qm = new QuizMutation();
        qm.setAssessmentId(quizId);

        quizController
                .aiGenerateQuestions(context, loggedInUser, qm);

        Date endMain = new Date();

        // less than delay , test non-blocking behavior
        assert (endMain.getTime() - now.getTime()) < (delay + 2000);


        // assert generateQuizQuestions called after delay of 200ms
        Thread.sleep(200);
        verify(aiQuizGenerationService, times(1))
                .generateQuizQuestions(any(), any(), any());





    }
}
