package org.piyush;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.logging.Logger;

@ExtendWith(MockitoExtension.class)
class AppTest {

  @Test
  void shouldLogWelcomeMessage() {
    try (MockedStatic<Logger> mockedLogger = Mockito.mockStatic(Logger.class)) {
      Logger loggerMock = Mockito.mock(Logger.class);
      mockedLogger.when(() -> Logger.getLogger(App.class.getName())).thenReturn(loggerMock);

      App.main(new String[]{});

      Mockito.verify(loggerMock).info("Task Management Up and Running!");
    }
  }
}