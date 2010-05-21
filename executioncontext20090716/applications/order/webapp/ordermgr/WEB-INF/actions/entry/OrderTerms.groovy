/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.ofbiz.entity.*;
import org.ofbiz.base.util.*;
import org.ofbiz.order.shoppingcart.*;
import org.ofbiz.party.contact.*;
import org.ofbiz.product.catalog.*;

cart = session.getAttribute("shoppingCart");
context.cart = cart;

orderTerms = cart.getOrderTerms();
context.orderTerms = orderTerms;

termTypes = delegator.findList("TermType", null, null, null, null,false);
context.termTypes = termTypes;
createNew = request.getParameter("createNew");

termIndex = request.getParameter("termIndex");
if ("Y".equals(createNew) && (!"-1".equals(termIndex))) {
    context.termIndex = termIndex;
    orderTerm = orderTerms[Integer.parseInt(termIndex)];
    if (orderTerm) {
       context.termTypeId = orderTerm.termTypeId;
       context.termValue = orderTerm.termValue;
       context.termDays = orderTerm.termDays;
       context.textValue = orderTerm.textValue;
    }
}