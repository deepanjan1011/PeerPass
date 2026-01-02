import Link from 'next/link';
import { FaGithub, FaTwitter } from 'react-icons/fa';

const Footer = () => {
    const currentYear = new Date().getFullYear();

    return (
        <footer className="w-full border-t border-border/60 bg-background/50 backdrop-blur-xl">
            <div className="container mx-auto px-4 py-8">
                <div className="flex flex-col md:flex-row items-center justify-between gap-4">

                    {/* Copyright / Brand - De-emphasized */}
                    <div className="flex items-center space-x-3 opacity-60 hover:opacity-100 transition-opacity duration-300">
                        <div className="w-7 h-7 bg-primary/10 rounded-md flex items-center justify-center">
                            <svg
                                className="w-3.5 h-3.5 text-primary"
                                fill="none"
                                stroke="currentColor"
                                strokeWidth="2.5"
                                viewBox="0 0 24 24"
                            >
                                <path strokeLinecap="round" strokeLinejoin="round" d="M13 10V3L4 14h7v7l9-11h-7z" />
                            </svg>
                        </div>
                        <span className="text-xs font-medium text-muted-foreground">
                            © {currentYear} PeerPass. Built for privacy.
                        </span>
                    </div>

                    {/* Socials */}
                    <div className="flex items-center space-x-4 text-muted-foreground">
                        <Link href="https://github.com/deepanjan1011/PeerPass" className="hover:text-primary transition-colors hover:scale-110 duration-200">
                            <FaGithub className="w-4 h-4" />
                        </Link>
                    </div>

                </div>
            </div>
        </footer>
    );
};

export default Footer;
