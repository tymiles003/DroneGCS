package com.dronegcs.console.perimeter_editor;

import com.dronedb.persistence.scheme.perimeter.CirclePerimeter;
import com.dronedb.persistence.scheme.perimeter.Perimeter;
import com.dronedb.persistence.scheme.perimeter.PolygonPerimeter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Created by oem on 3/27/17.
 */
@Component
public class PerimeterEditorFactoryImpl implements PerimeterEditorFactory {
    
    @Autowired
    private ApplicationContext applicationContext;
    
    @Override
    public <R extends Perimeter, T extends PerimeterEditor<R>> T getEditor(Class<R> clz) {
        if (PolygonPerimeter.class.getSimpleName().equals(clz.getSimpleName()))
            return applicationContext.getBean((Class<T>) PolygonPerimeterEditorImpl.class);

        if (CirclePerimeter.class.getSimpleName().equals(clz.getSimpleName()))
            return applicationContext.getBean((Class<T>) CirclePerimeterEditorImpl.class);

        System.out.println("Unrecoginzed class was requested '" + clz + "'");

        return null;
    }
}
