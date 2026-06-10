import type { MetadataRoute } from 'next';

export default function robots(): MetadataRoute.Robots {
  return {
    rules: [
      {
        userAgent: '*',
        allow: '/',
        disallow: ['/dashboard', '/dashboard/'],
      },
    ],
    sitemap: 'https://keepalive.akakash.me/sitemap.xml',
  };
}
