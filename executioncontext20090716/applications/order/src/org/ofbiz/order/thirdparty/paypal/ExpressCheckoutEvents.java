/*******************************************************************************
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
 *******************************************************************************/
package org.ofbiz.order.thirdparty.paypal;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.order.shoppingcart.ShoppingCart;
import org.ofbiz.order.shoppingcart.ShoppingCartEvents;
import org.ofbiz.product.store.ProductStoreWorker;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;


public class ExpressCheckoutEvents {

    public static final String resourceErr = "AccountingErrorUiLabels";
    public static final String module = ExpressCheckoutEvents.class.getName();
    public static enum CheckoutType {PAYFLOW, NONE};
    
    public static String setExpressCheckout(HttpServletRequest request, HttpServletResponse response) {
        Locale locale = UtilHttp.getLocale(request);
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        
        ShoppingCart cart = ShoppingCartEvents.getCartObject(request);
        CheckoutType checkoutType = determineCheckoutType(request);
        if (checkoutType.equals(CheckoutType.PAYFLOW)) {
            Map<String, ? extends Object> inMap = UtilMisc.toMap("userLogin", cart.getUserLogin(), "cart", cart);
            Map<String, Object> result = null;
            try {
                result = dispatcher.runSync("payflowSetExpressCheckout", inMap);
            } catch (GenericServiceException e) {
                request.setAttribute("_EVENT_MESSAGE_", UtilProperties.getMessage(resourceErr, "AccountingPayPalCommunicationError", locale));
                return "error";
            }
            if (ServiceUtil.isError(result)) {
                Debug.logError(ServiceUtil.getErrorMessage(result), module);
                request.setAttribute("_EVENT_MESSAGE_", ServiceUtil.getErrorMessage(result));
                return "error";
            }
        }
            
        return "success";
    }

    public static String expressCheckoutRedirect(HttpServletRequest request, HttpServletResponse response) {
        ShoppingCart cart = ShoppingCartEvents.getCartObject(request);
        String token = (String) cart.getAttribute("payPalCheckoutToken");
        if (UtilValidate.isEmpty(token)) {
            Debug.logError("No ExpressCheckout token found in cart, you must do a successful setExpressCheckout before redirecting.", module);
            return "error";
        }
        StringBuilder redirectUrl = new StringBuilder("https://www.sandbox.paypal.com/cgi-bin/webscr");
        redirectUrl.append("?cmd=_express-checkout&token=");
        redirectUrl.append(token);
        try {
            response.sendRedirect(redirectUrl.toString());
        } catch (IOException e) {
            Debug.logError(e, module);
            return "error";
        }
        return "success";
    }
    
    public static String getExpressCheckoutDetails(HttpServletRequest request, HttpServletResponse response) {
        Locale locale = UtilHttp.getLocale(request);
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        
        ShoppingCart cart = ShoppingCartEvents.getCartObject(request);
        CheckoutType checkoutType = determineCheckoutType(request);
        if (checkoutType.equals(CheckoutType.PAYFLOW)) {
            Map<String, ? extends Object> inMap = UtilMisc.toMap("userLogin", cart.getUserLogin(), "cart", cart);
            Map<String, Object> result = null;
            try {
                result = dispatcher.runSync("payflowGetExpressCheckout", inMap);
            } catch (GenericServiceException e) {
                request.setAttribute("_EVENT_MESSAGE_", UtilProperties.getMessage(resourceErr, "AccountingPayPalCommunicationError", locale));
                return "error";
            }
            if (ServiceUtil.isError(result)) {
                Debug.logError(ServiceUtil.getErrorMessage(result), module);
                request.setAttribute("_EVENT_MESSAGE_", ServiceUtil.getErrorMessage(result));
                return "error";
            }
        }
            
        return "success";
    }
    
    public static Map<String, Object> doExpressCheckout(String productStoreId, String orderId, GenericValue paymentPref, GenericValue userLogin, GenericDelegator delegator, LocalDispatcher dispatcher) {
        CheckoutType checkoutType = determineCheckoutType(delegator, productStoreId);
        if (checkoutType.equals(CheckoutType.PAYFLOW)) {
            Map<String, Object> inMap = FastMap.newInstance();
            inMap.put("userLogin", userLogin);
            inMap.put("orderPaymentPreference", paymentPref);
            Map<String, Object> result = null;
            try {
                result = dispatcher.runSync("payflowDoExpressCheckout", inMap);
            } catch (GenericServiceException e) {
                return ServiceUtil.returnError(e.getMessage());
            }
            return result;
        }
            
        return ServiceUtil.returnSuccess();
    }

    public static String expressCheckoutCancel(HttpServletRequest request, HttpServletResponse response) {
        ShoppingCart cart = ShoppingCartEvents.getCartObject(request);
        cart.removeAttribute("payPalCheckoutToken");
        return "success";
    }
    
    public static CheckoutType determineCheckoutType(HttpServletRequest request) {
        GenericDelegator delegator = (GenericDelegator) request.getAttribute("delegator");
        ShoppingCart cart = ShoppingCartEvents.getCartObject(request);
        return determineCheckoutType(delegator, cart.getProductStoreId());
    }

    public static CheckoutType determineCheckoutType(GenericDelegator delegator, String productStoreId) {
        GenericValue payPalPaymentSetting = ProductStoreWorker.getProductStorePaymentSetting(delegator, productStoreId, "EXT_PAYPAL", null, true);
        if (payPalPaymentSetting != null && payPalPaymentSetting.getString("paymentGatewayConfigId") != null) {
            try {
                GenericValue paymentGatewayConfig = payPalPaymentSetting.getRelatedOne("PaymentGatewayConfig");
                if (paymentGatewayConfig != null && "PAYFLOWPRO".equals(paymentGatewayConfig.getString("paymentGatewayConfigTypeId"))) {
                    return CheckoutType.PAYFLOW;
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
        }
        return CheckoutType.NONE;
    }
    
}