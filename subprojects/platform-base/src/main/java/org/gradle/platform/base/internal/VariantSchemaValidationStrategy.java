/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.platform.base.internal;

import org.gradle.api.Action;
import org.gradle.api.Named;
import org.gradle.api.Nullable;
import org.gradle.model.internal.manage.schema.ModelImplTypeSchema;
import org.gradle.model.internal.manage.schema.ModelProperty;
import org.gradle.model.internal.manage.schema.ModelSchema;
import org.gradle.model.internal.manage.schema.cache.ModelSchemaCache;
import org.gradle.model.internal.manage.schema.extract.InvalidManagedModelElementTypeException;
import org.gradle.model.internal.manage.schema.extract.ModelSchemaExtractionContext;
import org.gradle.model.internal.manage.schema.extract.ModelSchemaValidationStrategy;
import org.gradle.platform.base.Variant;

public class VariantSchemaValidationStrategy implements ModelSchemaValidationStrategy {
    @Nullable
    @Override
    public Action<? super ModelSchemaExtractionContext<?>> createValidator(ModelSchemaExtractionContext<?> extractionContext, final ModelSchema<?> schema, ModelSchemaCache cache) {
        if (!(schema instanceof ModelImplTypeSchema)) {
            return null;
        }

        return new Action<ModelSchemaExtractionContext<?>>() {
            @Override
            public void execute(ModelSchemaExtractionContext<?> extractionContext) {
                for (ModelProperty<?> property : ((ModelImplTypeSchema<?>) schema).getProperties().values()) {
                    if (property.isAnnotationPresent(Variant.class)) {
                        Class<?> propertyType = property.getType().getRawClass();
                        if (!String.class.equals(propertyType) && !Named.class.isAssignableFrom(propertyType)) {
                            throw invalidProperty(extractionContext, property, String.format("@Variant annotation only allowed for properties of type String and %s, but property has type %s", Named.class.getName(), propertyType.getName()));
                        }
                    }
                    if (property.getSetterAnnotations().keySet().contains(Variant.class)) {
                        throw invalidProperty(extractionContext, property, "@Variant annotation is only allowed on getter methods");
                    }
                }
            }
        };
    }

    protected InvalidManagedModelElementTypeException invalidProperty(ModelSchemaExtractionContext<?> extractionContext, ModelProperty<?> property, String message) {
        return new InvalidManagedModelElementTypeException(extractionContext, String.format("%s (invalid property: %s)", message, property.getName()));
    }
}
