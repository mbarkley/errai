/**
 * Copyright (C) 2016 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.errai.flow.cdi.api.example;

import org.jboss.errai.flow.api.CrudOperation;
import org.jboss.errai.flow.cdi.api.Flow;
import org.jboss.errai.flow.cdi.api.Stage;
import org.jboss.errai.flow.cdi.api.Transition;

/**
 *
 * @author Max Barkley <mbarkley@redhat.com>
 */
@Flow
public interface CreateNewEntity {

  @Stage(step = Loader.class, initial = true)
  @Transition(target = ListStage.class)
  interface LoadStage {
  }

  @Stage(step = EntityList.class)
  interface ListStage {
    @Transition
    default Class<?> transition(final CrudOperation op) {
      switch (op) {
      case CREATE:
        return SaveFlow.class;
      case UPDATE:
        return UpdateFlow.class;
      default:
        return CreateNewEntity.class;
      }
    }
  }

  @Flow
  interface SaveFlow {
    @Stage(step = EntityForm.class, initial = true)
    @Transition(target = Save.class)
    interface Form {
    }

    @Stage(step = Saver.class)
    interface Save {
    }
  }

  @Flow
  interface UpdateFlow {
    @Stage(step = EntityForm.class, initial = true)
    @Transition(target = Update.class)
    interface Form {
    }

    @Stage(step = Updater.class)
    interface Update {
    }
  }
}
