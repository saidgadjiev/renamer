package ru.gadjini.telegram.renamer.service.keyboard;

import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.telegram.renamer.common.CommandNames;
import ru.gadjini.telegram.renamer.common.MessagesProperties;
import ru.gadjini.telegram.renamer.model.bot.api.object.replykeyboard.InlineKeyboardMarkup;
import ru.gadjini.telegram.renamer.model.bot.api.object.replykeyboard.buttons.InlineKeyboardButton;
import ru.gadjini.telegram.renamer.request.Arg;
import ru.gadjini.telegram.renamer.request.RequestParams;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class InlineKeyboardService {

    private ButtonFactory buttonFactory;

    @Autowired
    public InlineKeyboardService(ButtonFactory buttonFactory) {
        this.buttonFactory = buttonFactory;
    }

    public InlineKeyboardMarkup getArchiveCreatingKeyboard(int jobId, Locale locale) {
        InlineKeyboardMarkup inlineKeyboardMarkup = inlineKeyboardMarkup();

        inlineKeyboardMarkup.getKeyboard().add(List.of(buttonFactory.cancelArchiveCreatingQuery(jobId, locale)));

        return inlineKeyboardMarkup;
    }

    public InlineKeyboardMarkup getThumbProcessingKeyboard(int jobId, Locale locale) {
        InlineKeyboardMarkup inlineKeyboardMarkup = inlineKeyboardMarkup();

        inlineKeyboardMarkup.getKeyboard().add(List.of(buttonFactory.cancelRenameQuery(jobId, locale)));

        return inlineKeyboardMarkup;
    }

    public InlineKeyboardMarkup getRenameProcessingKeyboard(int jobId, Locale locale) {
        InlineKeyboardMarkup inlineKeyboardMarkup = inlineKeyboardMarkup();

        inlineKeyboardMarkup.getKeyboard().add(List.of(buttonFactory.cancelRenameQuery(jobId, locale)));

        return inlineKeyboardMarkup;
    }

    public InlineKeyboardMarkup getFilesListKeyboard(Set<Integer> filesIds, int limit, int prevLimit, int offset, int unzipJobId, Locale locale) {
        InlineKeyboardMarkup inlineKeyboardMarkup = inlineKeyboardMarkup();

        if (!(offset == 0 && filesIds.size() == limit)) {
            if (filesIds.size() == offset + limit) {
                inlineKeyboardMarkup.getKeyboard().add(List.of(buttonFactory.toPrevPage(CommandNames.UNZIP_COMMAND_NAME, limit, Math.max(0, offset - prevLimit), locale)));
            } else if (offset == 0) {
                inlineKeyboardMarkup.getKeyboard().add(List.of(buttonFactory.toNextPage(CommandNames.UNZIP_COMMAND_NAME, limit, offset + limit, locale)));
            } else {
                inlineKeyboardMarkup.getKeyboard().add(List.of(buttonFactory.toPrevPage(CommandNames.UNZIP_COMMAND_NAME, limit, Math.max(0, offset - prevLimit), locale),
                        buttonFactory.toNextPage(CommandNames.UNZIP_COMMAND_NAME, limit, offset + limit, locale)));
            }
        }
        List<List<Integer>> lists = Lists.partition(filesIds.stream().skip(offset).limit(limit).collect(Collectors.toCollection(ArrayList::new)), 4);
        int i = offset + 1;
        for (List<Integer> list : lists) {
            List<InlineKeyboardButton> row = new ArrayList<>();

            for (int id : list) {
                row.add(buttonFactory.extractFileButton(String.valueOf(i++), id, unzipJobId));
            }

            inlineKeyboardMarkup.getKeyboard().add(row);
        }
        inlineKeyboardMarkup.getKeyboard().add(List.of(buttonFactory.extractAllButton(unzipJobId, locale)));

        return inlineKeyboardMarkup;
    }

    public InlineKeyboardMarkup getFilesListKeyboard(Set<Integer> filesIds, int unzipJobId, Locale locale) {
        InlineKeyboardMarkup inlineKeyboardMarkup = inlineKeyboardMarkup();

        int i = 1;
        List<List<Integer>> lists = Lists.partition(new ArrayList<>(filesIds), 4);
        for (List<Integer> list : lists) {
            List<InlineKeyboardButton> row = new ArrayList<>();

            for (int id : list) {
                row.add(buttonFactory.extractFileButton(String.valueOf(i++), id, unzipJobId));
            }

            inlineKeyboardMarkup.getKeyboard().add(row);
        }
        inlineKeyboardMarkup.getKeyboard().add(List.of(buttonFactory.extractAllButton(unzipJobId, locale)));

        return inlineKeyboardMarkup;
    }

    public InlineKeyboardMarkup getArchiveFilesKeyboard(Locale locale) {
        InlineKeyboardMarkup inlineKeyboardMarkup = inlineKeyboardMarkup();

        inlineKeyboardMarkup.getKeyboard().add(List.of(buttonFactory.cancelArchiveFiles(locale)));

        return inlineKeyboardMarkup;
    }

    public InlineKeyboardMarkup getUnzipProcessingKeyboard(int jobId, Locale locale) {
        InlineKeyboardMarkup inlineKeyboardMarkup = inlineKeyboardMarkup();

        inlineKeyboardMarkup.getKeyboard().add(List.of(buttonFactory.cancelUnzipQuery(jobId, locale)));

        return inlineKeyboardMarkup;
    }

    public InlineKeyboardMarkup getExtractFileProcessingKeyboard(int jobId, Locale locale) {
        InlineKeyboardMarkup inlineKeyboardMarkup = inlineKeyboardMarkup();

        inlineKeyboardMarkup.getKeyboard().add(List.of(buttonFactory.cancelExtractFileQuery(jobId, locale)));

        return inlineKeyboardMarkup;
    }

    public InlineKeyboardMarkup getResizeKeyboard(Locale locale, boolean cancelButton) {
        InlineKeyboardMarkup inlineKeyboardMarkup = inlineKeyboardMarkup();

        inlineKeyboardMarkup.getKeyboard().add(List.of(
                buttonFactory.delegateButton("16x16",
                        CommandNames.IMAGE_EDITOR_COMMAND_NAME, new RequestParams().add(Arg.IMAGE_SIZE.getKey(), "16x16")),
                buttonFactory.delegateButton("32x32",
                        CommandNames.IMAGE_EDITOR_COMMAND_NAME, new RequestParams().add(Arg.IMAGE_SIZE.getKey(), "32x32")),
                buttonFactory.delegateButton("64x64",
                        CommandNames.IMAGE_EDITOR_COMMAND_NAME, new RequestParams().add(Arg.IMAGE_SIZE.getKey(), "64x64"))));
        inlineKeyboardMarkup.getKeyboard().add(List.of(
                buttonFactory.delegateButton("480x360",
                        CommandNames.IMAGE_EDITOR_COMMAND_NAME, new RequestParams().add(Arg.IMAGE_SIZE.getKey(), "480x360")),
                buttonFactory.delegateButton("640x480",
                        CommandNames.IMAGE_EDITOR_COMMAND_NAME, new RequestParams().add(Arg.IMAGE_SIZE.getKey(), "640x480")),
                buttonFactory.delegateButton("1280x720",
                        CommandNames.IMAGE_EDITOR_COMMAND_NAME, new RequestParams().add(Arg.IMAGE_SIZE.getKey(), "1280x720"))));

        if (cancelButton) {
            inlineKeyboardMarkup.getKeyboard().add(List.of(
                    buttonFactory.cancelButton(locale),
                    buttonFactory.updateButton(locale)));

            inlineKeyboardMarkup.getKeyboard().add(List.of(
                    buttonFactory.delegateButton(MessagesProperties.GO_BACK_CALLBACK_COMMAND_DESCRIPTION,
                            CommandNames.IMAGE_EDITOR_COMMAND_NAME, new RequestParams().add(Arg.GO_BACK.getKey(), "g"), locale)));
        } else {
            inlineKeyboardMarkup.getKeyboard().add(List.of(
                    buttonFactory.updateButton(locale),
                    buttonFactory.delegateButton(MessagesProperties.GO_BACK_CALLBACK_COMMAND_DESCRIPTION,
                            CommandNames.IMAGE_EDITOR_COMMAND_NAME, new RequestParams().add(Arg.GO_BACK.getKey(), "g"), locale)));
        }

        return inlineKeyboardMarkup;
    }


    public InlineKeyboardMarkup getQueryDetailsKeyboard(int queryItemId, Locale locale) {
        InlineKeyboardMarkup inlineKeyboardMarkup = inlineKeyboardMarkup();

        inlineKeyboardMarkup.getKeyboard().add(List.of(buttonFactory.cancelQueryItem(queryItemId, CommandNames.QUERY_ITEM_DETAILS_COMMAND, locale)));
        inlineKeyboardMarkup.getKeyboard().add(List.of(buttonFactory.goBackCallbackButton(CommandNames.QUERIES_COMMAND, locale)));

        return inlineKeyboardMarkup;
    }

    public InlineKeyboardMarkup getQueriesKeyboard(List<Integer> queryItemsIds) {
        if (queryItemsIds.isEmpty()) {
            return null;
        }
        InlineKeyboardMarkup inlineKeyboardMarkup = inlineKeyboardMarkup();

        int i = 1;
        List<List<Integer>> lists = Lists.partition(queryItemsIds, 4);
        for (List<Integer> list : lists) {
            List<InlineKeyboardButton> row = new ArrayList<>();

            for (int queryItemId : list) {
                row.add(buttonFactory.queryItemDetails(String.valueOf(i++), queryItemId));
            }

            inlineKeyboardMarkup.getKeyboard().add(row);
        }

        return inlineKeyboardMarkup;
    }

    public InlineKeyboardMarkup reportKeyboard(int queueItemId, Locale locale) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.getKeyboard().add(List.of(buttonFactory.report(queueItemId, locale)));
        return inlineKeyboardMarkup;
    }

    private InlineKeyboardMarkup inlineKeyboardMarkup() {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();

        inlineKeyboardMarkup.setKeyboard(new ArrayList<>());

        return inlineKeyboardMarkup;
    }
}
