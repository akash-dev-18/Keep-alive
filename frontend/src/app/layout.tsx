// import type { Metadata } from 'next';
// import { ClerkProvider } from '@clerk/nextjs';
// import './globals.css';

// export const metadata: Metadata = {
//   title: 'KeepAlive — Uptime Monitoring for Small Teams',
//   description: 'Monitor HTTP endpoints, SSL certificates, and scheduled jobs from one clean operations workspace.',
// };

// export default function RootLayout({ children }: { children: React.ReactNode }) {
//   return (
//     <ClerkProvider>
//       <html lang="en">
//         <body className="font-sans antialiased">
//           {children}
//         </body>
//       </html>
//     </ClerkProvider>
//   );
// }


import type { Metadata } from 'next';
import { ClerkProvider } from '@clerk/nextjs';
import { Roboto_Mono } from 'next/font/google';
import './globals.css';

const robotoMono = Roboto_Mono({
  subsets: ['latin'],
  weight: ['300', '400', '500', '600', '700'],
  variable: '--font-roboto-mono',
});

export const metadata: Metadata = {
  title: 'KeepAlive — Uptime Monitoring for Small Teams',
  description: 'Monitor HTTP endpoints, SSL certificates, and scheduled jobs from one clean operations workspace.',
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <ClerkProvider>
      <html lang="en" className="scroll-smooth">
        <body className={`${robotoMono.variable} font-mono antialiased app-theme-dark`}>
          {children}
        </body>
      </html>
    </ClerkProvider>
  );
}
