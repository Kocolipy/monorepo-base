package com.example.backend.counter.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.backend.counter.application.UserCounterService;
import java.security.Principal;
import org.junit.jupiter.api.Test;

class UserCounterControllerTests {

    private final Principal principal = () -> "ada";

    @Test
    void getsCountForAuthenticatedUser() {
        RecordingCounterService service = new RecordingCounterService(3L);
        UserCounterController controller = new UserCounterController(service);

        UserCounterController.CountResponse response = controller.getCount(principal);

        assertThat(response.count()).isEqualTo(3L);
        assertThat(service.username).isEqualTo("ada");
    }

    @Test
    void incrementsCountForAuthenticatedUser() {
        RecordingCounterService service = new RecordingCounterService(4L);
        UserCounterController controller = new UserCounterController(service);

        UserCounterController.CountResponse response = controller.increment(principal);

        assertThat(response.count()).isEqualTo(4L);
        assertThat(service.username).isEqualTo("ada");
    }

    @Test
    void resetsCountForAuthenticatedUser() {
        RecordingCounterService service = new RecordingCounterService(0L);
        UserCounterController controller = new UserCounterController(service);

        UserCounterController.CountResponse response = controller.reset(principal);

        assertThat(response.count()).isZero();
        assertThat(service.username).isEqualTo("ada");
    }

    private static class RecordingCounterService extends UserCounterService {

        private final long result;
        private String username;

        RecordingCounterService(long result) {
            super(null, null);
            this.result = result;
        }

        @Override
        public long getCount(String username) {
            this.username = username;
            return result;
        }

        @Override
        public long increment(String username) {
            this.username = username;
            return result;
        }

        @Override
        public long reset(String username) {
            this.username = username;
            return result;
        }
    }
}
