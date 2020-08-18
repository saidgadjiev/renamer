package ru.gadjini.telegram.renamer.filter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.renamer.common.MessagesProperties;
import ru.gadjini.telegram.renamer.model.TgMessage;
import ru.gadjini.telegram.renamer.model.bot.api.method.send.HtmlMessage;
import ru.gadjini.telegram.renamer.model.bot.api.object.Update;
import ru.gadjini.telegram.renamer.model.bot.api.object.User;
import ru.gadjini.telegram.renamer.service.LocalisationService;
import ru.gadjini.telegram.renamer.service.SubscriptionService;
import ru.gadjini.telegram.renamer.service.UserService;
import ru.gadjini.telegram.renamer.service.message.MessageService;

import java.util.Locale;

@Component
public class SubscriptionFilter extends BaseBotFilter {

    private MessageService messageService;

    private LocalisationService localisationService;

    private UserService userService;

    private SubscriptionService subscriptionService;

    @Autowired
    public SubscriptionFilter(@Qualifier("messagelimits") MessageService messageService,
                              LocalisationService localisationService, UserService userService,
                              SubscriptionService subscriptionService) {
        this.messageService = messageService;
        this.localisationService = localisationService;
        this.userService = userService;
        this.subscriptionService = subscriptionService;
    }

    @Override
    public void doFilter(Update update) {
        if (subscriptionService.isChatMember(TgMessage.getUserId(update))) {
            super.doFilter(update);
        } else {
            sendNeedSubscription(TgMessage.getUser(update));
        }
    }

    private void sendNeedSubscription(User user) {
        Locale locale = userService.getLocaleOrDefault(user.getId());
        String msg = localisationService.getMessage(MessagesProperties.MESSAGE_NEED_SUBSCRIPTION, locale);
        messageService.sendMessage(new HtmlMessage((long) user.getId(), msg));
    }
}
