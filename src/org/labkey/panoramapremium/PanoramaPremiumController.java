/*
 * Copyright (c) 2019 LabKey Corporation. All rights reserved. No portion of this work may be reproduced in
 * any form or by any electronic or mechanical means without written permission from LabKey Corporation.
 */

package org.labkey.panoramapremium;

import org.labkey.api.action.SpringActionController;

public class PanoramaPremiumController extends SpringActionController
{
    private static final DefaultActionResolver _actionResolver = new DefaultActionResolver(PanoramaPremiumController.class);
    public static final String NAME = "panoramapremium";

    public PanoramaPremiumController()
    {
        setActionResolver(_actionResolver);
    }

}