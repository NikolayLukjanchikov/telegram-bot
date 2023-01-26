package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;

@Service
public class TelegramBotUpdatesListener implements UpdatesListener {
    private final String ERROR_TEXT = "Извини, я ограничен в ответах, напиши /start - для начала работы";
    private final String START_TEXT = "Приветствую, я бот который поможет не забыть";

    private Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);

    @Autowired
    private TelegramBot telegramBot;

    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(this);
    }

    @Override
    public int process(List<Update> updates) { // обработка запросов бота
        updates.forEach(update -> {
            logger.info("Processing update: {}", update);
            if ((update.message().text()).equals("/start")) {
                SendMessage startMessage = new SendMessage(update.message().chat().id(), START_TEXT);
                SendResponse startResponse = telegramBot.execute(startMessage);
            } else {
                SendMessage errorMessage = new SendMessage(update.message().chat().id(), ERROR_TEXT);
                SendResponse errorResponse = telegramBot.execute(errorMessage);
            }
        });
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

}
