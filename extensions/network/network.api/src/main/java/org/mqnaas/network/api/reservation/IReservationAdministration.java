package org.mqnaas.network.api.reservation;

/*
 * #%L
 * MQNaaS :: Network API
 * %%
 * Copyright (C) 2007 - 2015 Fundació Privada i2CAT, Internet i
 * 			Innovació a Catalunya
 * %%
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
 * #L%
 */

import java.util.Set;

import org.mqnaas.core.api.ICapability;
import org.mqnaas.core.api.IRootResource;
import org.mqnaas.network.api.request.Period;

/**
 * <p>
 * Capability managing the {@link IRootResource}s, {@link Period} and state of a {@link ReservationResource}
 * </p>
 * 
 * @author Adrián Roselló Rey (i2CAT)
 *
 */
public interface IReservationAdministration extends ICapability {

	public enum ReservationState {
		CREATED,
		PLANNED,
		RESERVED,
		FINISHED,
		CANCELLED
	}

	void setResources(Set<IRootResource> resources);

	void setPeriod(Period period);

	void setState(ReservationState state);

	Set<IRootResource> getResources();

	Period getPeriod();

	ReservationState getState();

}
