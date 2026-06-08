import { Webhook } from 'svix';
import { headers } from 'next/headers';

const webhookSecret = process.env.CLERK_WEBHOOK_SECRET || '';
const apiUrl = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

export async function POST(req: Request) {
  const headerPayload = await headers();
  const svixId = headerPayload.get('svix-id');
  const svixTimestamp = headerPayload.get('svix-timestamp');
  const svixSignature = headerPayload.get('svix-signature');

  if (!svixId || !svixTimestamp || !svixSignature) {
    return new Response('Missing svix headers', { status: 400 });
  }

  const payload = await req.text();

  // Verify signature before forwarding
  if (webhookSecret) {
    const wh = new Webhook(webhookSecret);
    try {
      wh.verify(payload, {
        'svix-id': svixId,
        'svix-timestamp': svixTimestamp,
        'svix-signature': svixSignature,
      });
    } catch {
      return new Response('Invalid webhook signature', { status: 400 });
    }
  }

  // Forward to Java backend which does the actual user creation/update/deletion
  try {
    const backendRes = await fetch(`${apiUrl}/api/webhooks/clerk`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'svix-id': svixId,
        'svix-timestamp': svixTimestamp,
        'svix-signature': svixSignature,
      },
      body: payload,
    });

    if (!backendRes.ok) {
      return new Response('Backend webhook processing failed', { status: 502 });
    }
  } catch {
    return new Response('Failed to reach backend', { status: 502 });
  }

  return new Response('OK', { status: 200 });
}
