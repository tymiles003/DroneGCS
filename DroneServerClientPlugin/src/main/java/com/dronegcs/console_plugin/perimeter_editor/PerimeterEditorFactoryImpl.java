package com.dronegcs.console_plugin.perimeter_editor;

import com.dronedb.persistence.scheme.CirclePerimeter;
import com.dronedb.persistence.scheme.Perimeter;
import com.dronedb.persistence.scheme.PolygonPerimeter;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Created by taljmars on 3/27/17.
 */
@Component
public class PerimeterEditorFactoryImpl implements PerimeterEditorFactory {

    private final static org.slf4j.Logger LOGGER = LoggerFactory.getLogger(PerimeterEditorFactoryImpl.class);
    
    @Autowired
    private ApplicationContext applicationContext;
    
    @Override
    public <R extends Perimeter, T extends PerimeterEditor<R>> T getEditor(Class<R> clz) {
        if (PolygonPerimeter.class.getSimpleName().equals(clz.getSimpleName()))
            return applicationContext.getBean((Class<T>) PolygonPerimeterEditorImpl.class);

        if (CirclePerimeter.class.getSimpleName().equals(clz.getSimpleName()))
            return applicationContext.getBean((Class<T>) CirclePerimeterEditorImpl.class);

        LOGGER.error("Unrecognized class was requested '" + clz + "'");

        return null;
    }
}
