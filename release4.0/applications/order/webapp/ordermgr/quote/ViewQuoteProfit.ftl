<#--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->
<#list quoteCoefficients as quoteCoefficient>
    <div class="tabletext">${quoteCoefficient.coeffName}:&nbsp;${quoteCoefficient.coeffValue}</div>
</#list>
<br/>
<div class="tableheadtext">${uiLabelMap.CommonTotalCostMult}:&nbsp;${costMult}</div>
<div class="tableheadtext">${uiLabelMap.CommonTotalCostToPriceMult}:&nbsp;${costToPriceMult}</div>
<br/>
<div class="tableheadtext">${uiLabelMap.CommonTotalCost}:&nbsp;<@ofbizCurrency amount=totalCost isoCode=quote.currencyUomId/></div>
<div class="tableheadtext">${uiLabelMap.CommonTotalAmount}:&nbsp;<@ofbizCurrency amount=totalPrice isoCode=quote.currencyUomId/></div>
<br/>
<div class="tableheadtext">${uiLabelMap.CommonTotalProfit}:&nbsp;<@ofbizCurrency amount=totalProfit isoCode=quote.currencyUomId/></div>
<div class="tableheadtext">${uiLabelMap.CommonTotalPercProfit}:&nbsp;${totalPercProfit}%</div>
<br/>