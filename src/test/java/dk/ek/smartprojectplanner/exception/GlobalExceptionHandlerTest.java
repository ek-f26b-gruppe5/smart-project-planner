package dk.ek.smartprojectplanner.exception;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Tests that exceptions from controllers become the right error pages. */
@WebMvcTest(GlobalExceptionHandlerTest.ThrowingController.class)
@Import(GlobalExceptionHandlerTest.ThrowingController.class)
class GlobalExceptionHandlerTest {

    /** Only exists in this test: each endpoint throws one kind of exception. */
    @Controller
    public static class ThrowingController {
        @GetMapping("/test/not-found")
        public String notFound() {
            throw new NotFoundException("Projektet findes ikke");
        }

        @GetMapping("/test/db-error")
        public String dbError() {
            throw new DataAccessResourceFailureException("Connection refused");
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void notFoundException_shows404Page() throws Exception {
        // Act + Assert (logged in, since every path is behind the login check)
        mockMvc.perform(get("/test/not-found").sessionAttr("userId", 1))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"));
    }

    @Test
    void databaseError_shows500Page() throws Exception {
        // Act + Assert
        mockMvc.perform(get("/test/db-error").sessionAttr("userId", 1))
                .andExpect(status().isInternalServerError())
                .andExpect(view().name("error/500"));
    }

    /**
     * Potential ideas for additional tests:
     * no leaks: the 500 page doesn't contain the exception's message or class name
     *           (content().string(not(containsString("Connection refused"))))
     * logging: the database error is logged (OutputCaptureExtension + CapturedOutput)
     * other errors: an exception we don't handle (e.g. IllegalStateException) falls through to Spring's error page
     */
}