package ru.gadjini.telegram.renamer.domain;

import ru.gadjini.telegram.renamer.model.Any2AnyFile;

public interface HasThumb {

    void setThumb(Any2AnyFile thumb);

    void delThumb();

    Any2AnyFile getThumb();
}
