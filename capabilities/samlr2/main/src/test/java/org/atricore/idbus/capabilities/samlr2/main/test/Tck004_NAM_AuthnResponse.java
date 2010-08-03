/*
 * Atricore IDBus
 *
 * Copyright (c) 2009, Atricore Inc.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.atricore.idbus.capabilities.samlr2.main.test;

/**
 * @author <a href="mailto:sgonzalez@atricore.org">Sebastian Gonzalez Oyuela</a>
 * @version $Id$
 */
public class Tck004_NAM_AuthnResponse {

    private String authnResponse = "<?xml version='1.0' encoding='UTF-8'?><ns3:Response xmlns:ns4=\"http://www.w3.org/2001/04/xmlenc#\" xmlns:ns3=\"urn:oasis:names:tc:SAML:2.0:protocol\" xmlns:ns2=\"http://www.w3.org/2000/09/xmldsig#\" xmlns=\"urn:oasis:names:tc:SAML:2.0:assertion\" Destination=\"http://josso01.dev.atricore.com:8181/IDBUS/SP-1/SAML2/ACS/POST\" IssueInstant=\"2009-06-20T22:41:33Z\" Version=\"2.0\" InResponseTo=\"id7B7EE8B23041CF11\" ID=\"idt2Qv2daQQbdFJqFdCCPJrOTu5qE\"><Issuer>http://nam01.dev.atricore.com:8080/nidp/saml2/metadata</Issuer><ns3:Status><ns3:StatusCode Value=\"urn:oasis:names:tc:SAML:2.0:status:Success\" /></ns3:Status><Assertion IssueInstant=\"2009-06-20T22:41:33Z\" ID=\"id60Oi4WQ7jBTsjftvq1QP4WrY2wE\" Version=\"2.0\"><Issuer>http://nam01.dev.atricore.com:8080/nidp/saml2/metadata</Issuer><ns2:Signature xmlns=\"http://www.w3.org/2000/09/xmldsig#\"><SignedInfo><CanonicalizationMethod Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\" /><SignatureMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#rsa-sha1\" /><Reference URI=\"#id60Oi4WQ7jBTsjftvq1QP4WrY2wE\"><Transforms><Transform Algorithm=\"http://www.w3.org/2000/09/xmldsig#enveloped-signature\" /><Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\" /></Transforms><DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\" /><DigestValue>l155FcIyXsM0fPe+Vp1H1ywPm/A=</DigestValue></Reference></SignedInfo><SignatureValue>ekx3zXy4b1c1dXG80aofxErP2tZ1qNqBFTzST3g/2nFLLR7+IUy9GHLHtBi2Vnf491S191KpA/hi2J9uBsdULxncAVWXTUpFfKifA1Cf+Pr9RuwsYcFtaSGZ4V7Tho8olWAUfoVW5Y0U7xfSylYj3OfTOQ+SNRQOVL2UD3ALVtmMnNDJpw+HSYBV4333nbLowWliA+rmzVKdB5a7o9J6Iw+B6sUVBawRJKxKzSNHzT0B3u6+y5kJlzKhz7ymZYGXuOnwvW/8vPKA5FVshitj2Zoyd8Sis9Cxqgdt0uP4irNdh6JGEvO1L+R7/l6PhnGFlV6MRzHn6uKajzLaVa6tKw==</SignatureValue><KeyInfo><X509Data><X509Certificate>MIIFJTCCBA2gAwIBAgIkAhwR/6TV13SCQflqv+LdVvJ05dmYfXF3XrNceJgPAgICMed1MA0GCSqGSIb3DQEBBQUAMDExGjAYBgNVBAsTEU9yZ2FuaXphdGlvbmFsIENBMRMwEQYDVQQKFApuYW0wMV90cmVlMB4XDTA5MDYxNjE4NDY1MloXDTExMDYxNjE4NDY1MlowQDEVMBMGA1UEAxMMdGVzdC1zaWduaW5nMRYwFAYDVQQLEw1hY2Nlc3NNYW5hZ2VyMQ8wDQYDVQQKEwZub3ZlbGwwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDZe1X8OEmxFMI26n+sBij5TnL7/Gc+aW8bZgG2q9/jOC5Fu0DFqAgfxVDVeQ+I4CR/178FODbOpF7RZBy21kbK7es4v6sDHex0xihWTm83GxIm3BG8uD1/1hjecFZ1H5NYoWte9OQ+QR5ti3XWaxRLGAYzx119dDx4QO7iW2OfYq7twCiyO0L2GnhZTNuDEd/tVrUrZrlzxEKamA3LxEdjcDV+2FMJEtNW13kmsov32s+rXWIjeRvAYSpkTbC1aPaGra4l53Kuj6P3RA/1iKqVdYqK9A4+WwozB0zo6azTkiyGrjUeepFSmZbmz3v8Gy4j7qCG5mnj2FVyMbZu7SSDAgMBAAGjggIUMIICEDAdBgNVHQ4EFgQUCP8hGp3wmmvRvI3hVTIKthPLcnUwHwYDVR0jBBgwFoAURtZ+FWnrP+JQpbqKnAb1feKa0wcwggHMBgtghkgBhvg3AQkEAQSCAbswggG3BAIBAAEB/xMdTm92ZWxsIFNlY3VyaXR5IEF0dHJpYnV0ZSh0bSkWQ2h0dHA6Ly9kZXZlbG9wZXIubm92ZWxsLmNvbS9yZXBvc2l0b3J5L2F0dHJpYnV0ZXMvY2VydGF0dHJzX3YxMC5odG0wggFIoBoBAQAwCDAGAgEBAgFGMAgwBgIBAQIBCgIBaaEaAQEAMAgwBgIBAQIBADAIMAYCAQECAQACAQCiBgIBFwEB/6OCAQSgWAIBAgICAP8CAQADDQCAAAAAAAAAAAAAAAADCQCAAAAAAAAAADAYMBACAQACCH//////////AQEAAgQG8N9IMBgwEAIBAAIIf/////////8BAQACBAbw30ihWAIBAgICAP8CAQADDQBAAAAAAAAAAAAAAAADCQBAAAAAAAAAADAYMBACAQACCH//////////AQEAAgQR/6TVMBgwEAIBAAIIf/////////8BAQACBBH/pNWiTjBMAgECAgEAAgIA/wMNAIAAAAAAAAAAAAAAAAMJAIAAAAAAAAAAMBIwEAIBAAIIf/////////8BAQAwEjAQAgEAAgh//////////wEBADANBgkqhkiG9w0BAQUFAAOCAQEAiXFEOlmv/OpRCptx/O3udZ5oHBpYxD/GEvhPtPea67GGChcpyqgaDhEEs3bJshk8Wv2JhCccV/QfeE5lLWciKelx/idEg+i+bxoVvHP2Vwt/Ym3cV2wh75ccCHCFU1bd0CZghTrMGoQ6pN/YKTtfH7GDDUL9CS921vx28cIBVNdCsiYd1VbKA2Y9Rw7fIUsesdoFgk+vHxiKZ0FPCpzDyqHcntM4KVsE4P1uC8sOnbsYzcKW86wOod9izLrsjljWrCeUs3U34pnnWNBrkwYRUbevBMURWTzIwYGBVPSALk5mzev9niYF/KOmcszm5jSKQUNHii+iB5+N/++BjZsQeg==</X509Certificate></X509Data></KeyInfo></ns2:Signature><Subject><NameID SPNameQualifier=\"http://josso01.dev.atricore.com:8181/IDBUS/SAML2/MD\" NameQualifier=\"http://nam01.dev.atricore.com:8080/nidp/saml2/metadata\" Format=\"urn:oasis:names:tc:SAML:2.0:nameid-format:persistent\">ZQQnRjNJVIZKeQxbI3QDWyR2AVUoewBekfe2DQ==</NameID><SubjectConfirmation Method=\"urn:oasis:names:tc:SAML:2.0:cm:bearer\"><SubjectConfirmationData InResponseTo=\"id7B7EE8B23041CF11\" Recipient=\"http://josso01.dev.atricore.com:8181/IDBUS/SP-1/SAML2/ACS/POST\" NotOnOrAfter=\"2009-06-20T23:41:33Z\" /></SubjectConfirmation></Subject><Conditions NotOnOrAfter=\"2009-06-20T22:46:33Z\" NotBefore=\"2009-06-20T22:36:33Z\"><AudienceRestriction><Audience>http://josso01.dev.atricore.com:8181/IDBUS/SAML2/MD</Audience></AudienceRestriction></Conditions><AuthnStatement SessionNotOnOrAfter=\"2009-06-20T23:41:33Z\" SessionIndex=\"72DEB88E5726C8D4F11837B69E982E73\" AuthnInstant=\"2009-06-20T22:40:34Z\"><AuthnContext><AuthnContextClassRef>urn:oasis:names:tc:SAML:2.0:ac:classes:Password</AuthnContextClassRef><AuthnContextDeclRef>name/password/uri</AuthnContextDeclRef></AuthnContext></AuthnStatement></Assertion></ns3:Response>";

    
}
