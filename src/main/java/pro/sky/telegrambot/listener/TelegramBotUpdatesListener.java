package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.model.NotificationTask;
import pro.sky.telegrambot.repository.NotificationTaskRepository;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TelegramBotUpdatesListener implements UpdatesListener {
    private final NotificationTaskRepository notificationTaskRepository;
    private final String START = "/start";
    private final String ERROR_MESSAGE = "Извини, я ограничен в ответах, напиши /start - для начала работы";
    private final String START_MESSAGE = "Приветствую, я бот, который поможет не забыть. Введи время и сообщение " + "Пример: 01.01.2022 20:00 Сделать домашнюю работу";
    private final Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);

    @Autowired
    private TelegramBot telegramBot;

    public TelegramBotUpdatesListener(NotificationTaskRepository notificationTaskRepository) {
        this.notificationTaskRepository = notificationTaskRepository;
    }

    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(this);
    }

    @Override
    public int process(List<Update> updates) {
        // обработка запросов бота ([0-9\.\:\s]{16})(\s)([\W+]+)

        Pattern pattern = Pattern.compile("([0-9\\.\\:\\s]{16})(\\s)([\\W+]+)");
        updates.forEach(update -> {
            logger.info("Processing update: {}", update);
            Matcher matcher = pattern.matcher(update.message().text()); // проверяем сообщение на соответствие паттерну
            if (matcher.matches()) {
                String date = matcher.group(1);
                String task = matcher.group(3);
                String reminderReturnMessage = ("Принято!  Напомню тебе " + date + " вот об этом: " + task);
                LocalDateTime timeToRemind = LocalDateTime.parse(date, DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
                telegramBot.execute(new SendMessage(update.message().chat().id(), reminderReturnMessage));

                //создаём нотификацию
                NotificationTask notificationTask = new NotificationTask();
                notificationTask.setChatId(update.message().chat().id());
                notificationTask.setTimeDate(timeToRemind);
                notificationTask.setMessage(task);

                //сохраняем нотификацию в БД
                notificationTaskRepository.save(notificationTask);


            } else if ((update.message().text()).equals(START)) {
                SendMessage startMessage = new SendMessage(update.message().chat().id(), START_MESSAGE);
                telegramBot.execute(startMessage);
            } else {
                SendMessage errorMessage = new SendMessage(update.message().chat().id(), ERROR_MESSAGE);
                telegramBot.execute(errorMessage);
            }
        });
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

    //ежеминутный поиск и отправка заданий
    @Scheduled(cron = "0 0/1 * * * *")
    public void sendingScheduledTask() {
        //получаем текущее время округлённое до минут
        LocalDateTime timeToSending = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        List<NotificationTask> notificationTasks = notificationTaskRepository.findNotificationTaskByTimeDate(timeToSending);
        if (!notificationTasks.isEmpty()) {
            notificationTasks.parallelStream()
                    .forEach(this::send);
        }
    }

    //отправка нотификаций
    private void send(NotificationTask task) {
        telegramBot.execute(new SendMessage(task.getChatId(), task.getMessage()));
    }
}
