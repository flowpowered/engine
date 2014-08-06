/*
 * This file is part of Flow Engine, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2013 Spout LLC <http://www.spout.org/>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.flowpowered.engine.physics;

import org.spout.physics.constraint.ContactPoint;
import org.spout.physics.engine.EventListener;

public final class FlowEventListener implements EventListener {

    @Override
    public void beginContact(ContactPoint.ContactPointInfo contactInfo) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    public void newContact(ContactPoint.ContactPointInfo contactInfo) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

/*
    @Override
    public boolean onCollide(CollisionBody body1, CollisionBody body2, ContactInfo contactInfo) {
        final Object user1 = body1.getUserPointer();
        final Object user2 = body2.getUserPointer();

        //Step 1 - Only do callbacks if entities' involved haven't been removed
        if ((user1 instanceof Entity && ((Entity) user1).isRemoved()) || (user2 instanceof Entity && ((Entity) user2).isRemoved())) {
            return super.onCollide(body1, body2, contactInfo);
        }

        //Step 2 - Events
        final SpoutContactInfo info = new SpoutContactInfo(contactInfo);

        EntityCollideEvent event = null;

        if (user1 instanceof Entity) {
            if (user2 instanceof Entity) {
                event = new EntityCollideEntityEvent((Entity) user1, (Entity) user2, info);
            } else {
            event = new EntityCollideBlockEvent((Entity) user1, (Block) user2, info);
            }
        } else if (user1 instanceof Block) {
            if (user2 instanceof Entity) {
                event = new EntityCollideBlockEvent((Entity) user2, (Block) user1, info);
            }
        }

        //Events have priority over callbacks as well as ghost status!
        if (event != null && Spout.getEventManager().callEvent(event).isCancelled()) {
            return true;
        }

        //Step 3 - Callbacks
        if (user1 instanceof Entity) {
            for (Component component : ((Entity) user1).values()) {
                if (component instanceof EntityComponent) {
                    ((EntityComponent) component).onCollided(event);
                }
            }
        }

        if (user2 instanceof Entity) {
            for (Component component : ((Entity) user2).values()) {
                if (component instanceof EntityComponent) {
                    ((EntityComponent) component).onCollided(event);
                }
            }
        }

        //Step 4 - Groups (TODO: Implement and make step 2 this)

        //TODO Support collision groups
        return super.onCollide(body1, body2, contactInfo);
    }
*/
}
